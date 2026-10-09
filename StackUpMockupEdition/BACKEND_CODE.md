## src/backend/Account.java

```java
package backend;

import java.io.Serializable;
/** ENCAPSULATION: the password is never stored, only a salted hash that stays package-private. */
public final class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username, role, salt, hash;

    public Account(String username, String role, String salt, String hash) {
        this.username = username;
        this.role = role;
        this.salt = salt;
        this.hash = hash;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
    String getSalt() {
        return salt;
    }
    String getHash() {
        return hash;
    }
}

```

## src/backend/ChartData.java

```java
package backend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Converts transaction records into chart totals. Contains no Swing code. */
public final class ChartData {

    private ChartData() {}

    public static BigDecimal total(List<FinancialRecord> records, String type) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinancialRecord record : records) {
            if (record.getType().equals(type))
                total = total.add(record.getAmount());
        }
        return total;
    }

    /** Profit of a list of records: revenues add, expenses subtract (each record decides its own sign). */
    public static BigDecimal net(List<FinancialRecord> records) {
        BigDecimal net = BigDecimal.ZERO;
        for (FinancialRecord record : records) net = net.add(record.signedAmount());
        return net;
    }

    public static Map<String, BigDecimal> categories(List<FinancialRecord> records, String type) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (FinancialRecord record : records) {
            if (record.getType().equals(type)) {
                String category = record.getCategory();
                totals.put(category,
                    totals.getOrDefault(category, BigDecimal.ZERO).add(record.getAmount()));
            }
        }
        return totals;
    }

    public static List<FinancialRecord> between(
        List<FinancialRecord> records, LocalDate from, LocalDate to) {
        List<FinancialRecord> matching = new ArrayList<>();
        for (FinancialRecord record : records) {
            LocalDate date = record.getDate().toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to))
                matching.add(record);
        }
        return matching;
    }

    public static BigDecimal[][] monthly(List<FinancialRecord> records, int year) {
        BigDecimal[][] amounts = new BigDecimal[3][12];
        for (int series = 0; series < 3; series++) Arrays.fill(amounts[series], BigDecimal.ZERO);
        for (FinancialRecord record : records) {
            if (record.getDate().getYear() == year) {
                int month = record.getDate().getMonthValue() - 1;
                int series = record.signedAmount().signum() > 0 ? 0 : 1;   // polymorphism: each record knows its sign
                amounts[series][month] = amounts[series][month].add(record.getAmount());
            }
        }
        for (int month = 0; month < 12; month++)
            amounts[2][month] = amounts[0][month].subtract(amounts[1][month]);
        return amounts;
    }
}

```

## src/backend/Expense.java

```java
package backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/** INHERITANCE: Expense is a FinancialRecord whose amount takes away from profit. */
public final class Expense extends FinancialRecord {

    private static final long serialVersionUID = 1L;

    public Expense(String description, String category, BigDecimal amount, LocalDateTime date,
        String recordedBy) {
        super(description, category, amount, date, recordedBy);
    }

    @Override

    public String getType() {
        return "Expense";
    }

    @Override
    public BigDecimal signedAmount() {
        return getAmount().negate();
    }
}

```

## src/backend/FinancialRecord.java

```java
package backend;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
/**
 * ABSTRACTION: the idea of "a money record". It cannot be created on its own, only as a
 * Revenue or an Expense. ENCAPSULATION: the fields are private and read through getters.
 */
public abstract class FinancialRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();

    private final String description, category, recordedBy;

    private final BigDecimal amount;

    private final LocalDateTime date;

    protected FinancialRecord(String description, String category, BigDecimal amount,
        LocalDateTime date, String recordedBy) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Description is required.");
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Category is required.");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException(
                "Enter a positive amount with up to two decimal places.");
        this.description = description.trim();
        this.category = category.trim();
        this.amount = amount;
        this.date = java.util.Objects.requireNonNull(date);
        this.recordedBy = recordedBy;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    /** Each subclass says what it is (polymorphism). */
    public abstract String getType();

    /** The amount as it affects profit: positive for revenue, negative for an expense. */
    public abstract BigDecimal signedAmount();
}

```

## src/backend/FinancialService.java

```java
package backend;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Single shared service: every window reads the same saved records (Singleton). */
public final class FinancialService {

    private static FinancialService instance;

    private final Path file;

    private State state;

    private Account currentAccount;

    private static class State implements Serializable {
        private static final long serialVersionUID = 1L;
        ArrayList<Account> accounts = new ArrayList<>();
        ArrayList<FinancialRecord> records = new ArrayList<>();
        ArrayList<Payment> payments = new ArrayList<>();
        ArrayList<String> activity = new ArrayList<>();
    }

    private FinancialService(Path file) throws IOException {
        this.file = file;
        state = new State();
        if (Files.exists(file))
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
                in.setObjectInputFilter(
                    ObjectInputFilter.Config.createFilter("java.base/*;backend.*;!*"));
                state = (State) in.readObject();
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new IOException("Cannot read saved data.", e);
            }
    }

    public static synchronized FinancialService getInstance() throws IOException {
        if (instance == null)
            instance =
                new FinancialService(Paths.get(System.getProperty("fms.data", "data/finance.dat")));
        return instance;
    }

    private void save(State next) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "finance-", ".tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
                out.writeObject(next);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            state = next;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private State copy() {
        State nextState = new State();
        nextState.accounts.addAll(state.accounts);
        nextState.records.addAll(state.records);
        nextState.payments.addAll(state.payments);
        nextState.activity.addAll(state.activity);
        return nextState;
    }

    private static String hash(char[] password, String salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, Base64.getDecoder().decode(salt), 120000, 256);
        try {
            return Base64.getEncoder().encodeToString(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded());
        } finally {
            spec.clearPassword();
        }
    }

    public boolean hasAccounts() {
        return !state.accounts.isEmpty();
    }

    public void createAccount(String username, char[] password, String role) throws Exception {
        username = username.trim();
        if (username.isBlank() || password.length < 8)
            throw new IllegalArgumentException(
                "Enter a username and a password of at least 8 characters.");
        if (!role.equals("Admin") && !role.equals("Employee"))
            throw new IllegalArgumentException("Invalid role.");
        if (hasAccounts())
            require("Admin");
        else if (!role.equals("Admin"))
            throw new IllegalArgumentException("The first account must be Admin.");
        for (Account account : state.accounts)
            if (account.getUsername().equalsIgnoreCase(username))
                throw new IllegalArgumentException("Username already exists.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        State nextState = copy();
        nextState.accounts.add(
            new Account(username, role, encodedSalt, hash(password, encodedSalt)));
        save(nextState);
    }

    /** Only admins can inspect account names and roles; password hashes remain private. */
    public List<Account> getAccounts() {
        require("Admin");
        return List.copyOf(state.accounts);
    }

    public void resetEmployeePassword(String username, char[] password) throws Exception {
        require("Admin");
        if (password.length < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        State next = copy();
        for (int index = 0; index < next.accounts.size(); index++) {
            Account account = next.accounts.get(index);
            if (account.getUsername().equalsIgnoreCase(username)) {
                if (!account.getRole().equals("Employee"))
                    throw new IllegalArgumentException("Select an employee account.");
                byte[] salt = new byte[16];
                new SecureRandom().nextBytes(salt);
                String encoded = Base64.getEncoder().encodeToString(salt);
                next.accounts.set(index, new Account(account.getUsername(), account.getRole(),
                    encoded, hash(password, encoded)));
                log(next, "Reset employee password | " + account.getUsername());
                save(next);
                return;
            }
        }
        throw new IllegalArgumentException("Employee account not found.");
    }

    public Account login(String username, char[] password) throws Exception {
        for (Account account : state.accounts)
            if (account.getUsername().equalsIgnoreCase(username.trim())
                && MessageDigest.isEqual(Base64.getDecoder().decode(account.getHash()),
                    Base64.getDecoder().decode(hash(password, account.getSalt())))) {
                currentAccount = account;
                return account;
            }
        throw new IllegalArgumentException("Incorrect username or password.");
    }

    public void logout() {
        currentAccount = null;
    }

    public Account getCurrent() {
        return currentAccount;
    }

    private void require(String role) {
        if (currentAccount == null || !currentAccount.getRole().equals(role))
            throw new SecurityException(role + " access required.");
    }

    private void signedIn() {
        if (currentAccount == null)
            throw new SecurityException("Please sign in.");
    }

    private void log(State nextState, String message) {
        nextState.activity.add(0,
            LocalDateTime.now().withNano(0) + " | " + currentAccount.getUsername() + " | "
                + message);
    }

    public List<FinancialRecord> getRecords() {
        signedIn();
        return List.copyOf(state.records);
    }

    public List<Payment> getPayments() {
        signedIn();
        return List.copyOf(state.payments);
    }

    public List<String> getActivity() {
        require("Admin");
        return List.copyOf(state.activity);
    }

    public void addRecord(FinancialRecord record) throws IOException {
        require("Employee");
        State nextState = copy();
        nextState.records.add(record);
        log(nextState, "Recorded " + record.getType() + ": " + record.getDescription());
        save(nextState);
    }

    public void editRecord(String id, FinancialRecord replacement) throws IOException {
        require("Admin");
        State nextState = copy();
        int i = index(id);
        nextState.records.set(i, replacement);
        log(nextState, "Edited transaction: " + replacement.getDescription());
        save(nextState);
    }

    public void deleteRecord(String id) throws IOException {
        require("Admin");
        State nextState = copy();
        int i = index(id);
        FinancialRecord record = nextState.records.remove(i);
        log(nextState, "Deleted transaction: " + record.getDescription());
        save(nextState);
    }

    private int index(String id) {
        for (int i = 0; i < state.records.size(); i++)
            if (state.records.get(i).getId().equals(id))
                return i;
        throw new IllegalArgumentException("Transaction not found.");
    }

    public void schedule(Payment payment) throws IOException {
        require("Employee");
        State nextState = copy();
        nextState.payments.add(payment);
        log(nextState, "Scheduled payment: " + payment.getDescription());
        save(nextState);
    }

    public void pay(String id) throws IOException {
        require("Employee");
        State nextState = copy();
        for (int i = 0; i < nextState.payments.size(); i++) {
            Payment payment = nextState.payments.get(i);
            if (payment.getId().equals(id)) {
                if (payment.isPaid())
                    throw new IllegalArgumentException("Payment is already paid.");
                nextState.payments.set(i, payment.asPaid());
                nextState.records.add(new Expense(payment.getDescription(), payment.getCategory(),
                    payment.getAmount(), LocalDateTime.now(), currentAccount.getUsername()));
                log(nextState, "Paid: " + payment.getDescription());
                save(nextState);
                return;
            }
        }
        throw new IllegalArgumentException("Payment not found.");
    }

    public BigDecimal total(String type, LocalDate from, LocalDate to) {
        signedIn();
        BigDecimal value = BigDecimal.ZERO;
        for (FinancialRecord record : state.records)
            if (record.getType().equals(type) && !record.getDate().toLocalDate().isBefore(from)
                && !record.getDate().toLocalDate().isAfter(to))
                value = value.add(record.getAmount());
        return value;
    }
}

```

## src/backend/Payment.java

```java
package backend;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public final class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id, description, category;

    private final BigDecimal amount;

    private final LocalDate due;

    private final boolean paid;

    /** Who scheduled it; null for payments saved before this was tracked. */
    private final String scheduledBy;

    public Payment(String description, String category, BigDecimal amount, LocalDate due) {
        this(UUID.randomUUID().toString(), description, category, amount, due, false, null);
    }

    public Payment(String description, String category, BigDecimal amount, LocalDate due,
        String scheduledBy) {
        this(UUID.randomUUID().toString(), description, category, amount, due, false, scheduledBy);
    }

    public String getScheduledBy() {
        return scheduledBy;
    }

    private Payment(String id, String description, String category, BigDecimal amount,
        LocalDate due, boolean paid, String scheduledBy) {
        if (description.isBlank() || category.isBlank() || amount.signum() <= 0
            || amount.scale() > 2)
            throw new IllegalArgumentException(
                "Description, category and a positive amount are required.");
        this.description = description.trim();
        this.category = category.trim();
        this.amount = amount;
        this.due = java.util.Objects.requireNonNull(due);
        this.paid = paid;
        this.scheduledBy = scheduledBy;
        this.id = id;
    }

    public Payment asPaid() {
        return new Payment(id, description, category, amount, due, true, scheduledBy);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDue() {
        return due;
    }

    public boolean isPaid() {
        return paid;
    }
}

```

## src/backend/Revenue.java

```java
package backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/** INHERITANCE: Revenue is a FinancialRecord whose amount adds to profit. */
public final class Revenue extends FinancialRecord {

    private static final long serialVersionUID = 1L;

    public Revenue(String description, String category, BigDecimal amount, LocalDateTime date,
        String recordedBy) {
        super(description, category, amount, date, recordedBy);
    }

    @Override

    public String getType() {
        return "Revenue";
    }

    @Override
    public BigDecimal signedAmount() {
        return getAmount();
    }
}

```

