## src/Main.java

```java
import backend.FinancialService;
import frontend.LoginUI;
import frontend.style.Theme;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

public final class Main {
    /** The tallest page needs about this many device pixels of window height at 100% zoom. */
    private static final double TALLEST_PAGE = 1010;

    public static void main(String[] args) {
        if (relaunchZoomedOut()) return;
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.install();
                new LoginUI(FinancialService.getInstance()).setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,exception.getMessage(),"Startup error",JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Java reads its UI scale once at start-up. On short screens the app restarts itself
     * zoomed out, so every page fits without scrolling. Returns true when it did so.
     */
    private static boolean relaunchZoomedOut() {
        if (System.getProperty("sun.java2d.uiScale") != null || GraphicsEnvironment.isHeadless()) return false;
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        double system = environment.getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleY();
        Rectangle usable = environment.getMaximumWindowBounds();
        double fit = usable.height * system / TALLEST_PAGE;
        if (fit >= system) return false;
        try {
            String java = ProcessHandle.current().info().command().orElse(null);
            if (java == null) return false;
            List<String> command = new ArrayList<>(List.of(java,
                "-Dsun.java2d.uiScale=" + String.format(Locale.US, "%.2f", Math.max(fit, 0.6))));
            if (System.getProperty("fms.data") != null) command.add("-Dfms.data=" + System.getProperty("fms.data"));
            command.addAll(List.of("-cp", System.getProperty("java.class.path"), "Main"));
            new ProcessBuilder(command).inheritIO().start();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}

```

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

## src/frontend/AccountsUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Create accounts, see who has access, and reset an employee's password. */
public final class AccountsUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private String role = "Employee";

    public AccountsUI(DashboardUI window) {
        super(window);
        JPanel left = Theme.clear(new BorderLayout(0, 12));
        left.add(createCard(), BorderLayout.NORTH);

        JPanel right = Theme.clear(new BorderLayout(0, 12));
        DefaultTableModel rows = model("Username", "Role", "Access");
        JTable accounts = table(rows, "user", "chip", "muted");
        widths(accounts, 160, 100, 220);
        accounts.setRowHeight(48);
        JComboBox<String> employees = new JComboBox<>();
        JButton reset = Theme.dark("Reset Password");
        JTextField search = Theme.search("Search users");
        search.setPreferredSize(new Dimension(260, 44));
        JLabel count = Theme.muted("", 15);
        Runnable refresh = () -> {
            String needle = search.getText().trim().toLowerCase(Locale.ROOT);
            rows.setRowCount(0);
            employees.removeAllItems();
            int total = 0;
            for (Account account : service.getAccounts()) {
                total++;
                if (account.getRole().equals("Employee")) employees.addItem(account.getUsername());
                if (!account.getUsername().toLowerCase(Locale.ROOT).contains(needle)) continue;
                rows.addRow(new Object[]{account.getUsername(), account.getRole(),
                    account.getRole().equals("Admin") ? "Full access" : "Records and payments"});
            }
            count.setText("· " + total + (total == 1 ? " user" : " users"));
            reset.setEnabled(employees.getItemCount() > 0);
        };
        JPanel registered = Theme.card();
        registered.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        JPanel title = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.add(Theme.label("Registered Accounts", 22, Theme.INK));
        title.add(count);
        head.add(title, BorderLayout.WEST);
        head.add(search, BorderLayout.EAST);
        registered.add(head, BorderLayout.NORTH);
        registered.add(tableArea(accounts, "No registered accounts."));
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { refresh.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { refresh.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { }
        });
        right.add(registered, BorderLayout.CENTER);

        JPasswordField replacement = passwordField("At least 8 characters");
        reset.addActionListener(event -> {
            String employee = (String) employees.getSelectedItem();
            if (employee == null) return;
            char[] value = replacement.getPassword();
            try {
                service.resetEmployeePassword(employee, value);
                Theme.message(this, "Password updated", employee + " signs in with the new password next time.");
            } catch (Exception exception) {
                error(exception);
            } finally {
                Arrays.fill(value, '\0');
                replacement.setText("");
            }
        });
        accounts.getSelectionModel().addListSelectionListener(event -> {
            int row = accounts.getSelectedRow();
            if (row >= 0 && "Employee".equals(rows.getValueAt(row, 1))) employees.setSelectedItem(rows.getValueAt(row, 0));
        });
        JPanel resetCard = Theme.card();
        resetCard.setLayout(new BorderLayout(0, 14));
        JPanel words = Theme.clear(new GridLayout(2, 1, 0, 4));
        words.add(Theme.label("Reset Employee Password", 22, Theme.INK));
        words.add(Theme.muted("The employee signs in with the new password next time.", 14));
        resetCard.add(words, BorderLayout.NORTH);
        employees.setPreferredSize(new Dimension(200, 44));
        reset.setPreferredSize(new Dimension(190, 44));
        JPanel form = Theme.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.insets = new Insets(0, 0, 0, 14);
        c.gridx = 0; form.add(Theme.field("Employee", employees), c);
        c.gridx = 1; form.add(Theme.field("New password", replacement), c);
        c.gridx = 2; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0); c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.SOUTH;
        form.add(reset, c);
        resetCard.add(form);
        right.add(resetCard, BorderLayout.SOUTH);

        add(Theme.columns(12, new double[]{3.2, 6}, left, right));
        refresh.run();
    }


    @Override protected String subtitle() { return "Create user accounts, assign roles and reset employee passwords."; }

    private static JPasswordField passwordField(String hint) {
        Theme.RoundedPassword field = new Theme.RoundedPassword();
        field.setPlaceholder(hint);
        return field;
    }

    private JPanel createCard() {
        JTextField username = Theme.input("e.g. maria");
        JPasswordField password = passwordField("At least 8 characters");
        JButton save = Theme.button("Save Account");
        save.setPreferredSize(new Dimension(100, 42));
        JPanel roles = Theme.segmented(new String[]{"Employee", "Admin"}, 0, Theme.INK, index -> role = index == 0 ? "Employee" : "Admin");
        JPanel stretch = Theme.clear(new GridLayout(1, 1));
        stretch.add(roles);
        JPanel form = Theme.clear(new GridLayout(3, 1, 0, 8));
        form.add(Theme.field("Username", username));
        form.add(Theme.field("Password", password));
        form.add(Theme.field("Role", stretch));
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(Theme.label("Create Account", 22, Theme.INK), BorderLayout.NORTH);
        card.add(form);
        card.add(save, BorderLayout.SOUTH);
        save.addActionListener(event -> {
            char[] value = password.getPassword();
            try {
                service.createAccount(username.getText(), value, role);
                username.setText("");
                Theme.message(this, "Account created", "The new account can sign in right away.");
                window.refresh();
            } catch (Exception exception) {
                error(exception);
            } finally {
                Arrays.fill(value, '\0');
                password.setText("");
            }
        });
        return card;
    }

}

```

## src/frontend/ActivityLogUI.java

```java
package frontend;

import frontend.style.*;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** A record of every change made in StackUp, newest first. */
public final class ActivityLogUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private static final String[] FILTERS = {"All", "Created", "Edited", "Deleted", "Account"};

    private static final class Entry {
        final LocalDateTime time;
        final String user, kind, label, details;
        Entry(LocalDateTime time, String user, String kind, String label, String details) {
            this.time = time; this.user = user; this.kind = kind; this.label = label; this.details = details;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> matches = new ArrayList<>();
    private int filter;

    public ActivityLogUI(DashboardUI window) {
        super(window);
        for (String line : service.getActivity()) {
            String[] parts = line.split(" \\| ", 3);
            if (parts.length < 3) continue;
            try { entries.add(parse(LocalDateTime.parse(parts[0]), parts[1], parts[2])); }
            catch (RuntimeException ignored) { }
        }
        add(statCards(), BorderLayout.NORTH);

        DefaultTableModel rows = model("Action", "User", "Details", "Date");
        JTable table = table(rows, "chip", "user", "text", "muted");
        table.setRowHeight(44);
        widths(table, 110, 120, 420, 170);
        JTextField search = Theme.search("Search activity");
        search.setPreferredSize(new Dimension(280, 44));
        JComboBox<String> users = new JComboBox<>();
        users.addItem("All users");
        for (String user : new TreeSet<>(entries.stream().map(entry -> entry.user).toList())) users.addItem(user);
        users.setPreferredSize(new Dimension(160, 44));
        Runnable[] refresh = new Runnable[1];
        Pager pages = new Pager(5, () -> refresh[0].run());
        JPanel tabs = Theme.segmented(FILTERS, 0, Theme.INK, index -> { filter = index; pages.reset(); refresh[0].run(); });
        refresh[0] = () -> {
            String needle = search.getText().trim().toLowerCase(Locale.ROOT);
            matches.clear();
            for (Entry entry : entries) {
                if (filter > 0 && !entry.kind.equals(FILTERS[filter])) continue;
                if (users.getSelectedIndex() > 0 && !entry.user.equals(users.getSelectedItem())) continue;
                if (!(entry.details + " " + entry.user + " " + entry.label).toLowerCase(Locale.ROOT).contains(needle)) continue;
                matches.add(entry);
            }
            pages.update(matches.size());
            rows.setRowCount(0);
            for (Entry entry : matches.subList(pages.from(), pages.to()))
                rows.addRow(new Object[]{entry.label, entry.user, entry.details, entry.time.format(STAMP)});
        };
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { pages.reset(); refresh[0].run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { pages.reset(); refresh[0].run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { }
        });
        users.addActionListener(event -> { pages.reset(); refresh[0].run(); });

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout(12, 0));
        head.add(tabs, BorderLayout.WEST);
        JPanel right = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.add(search);
        right.add(users);
        head.add(right, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        card.add(tableArea(table, "No activity yet."));
        card.add(pages, BorderLayout.SOUTH);
        add(card);
        refresh[0].run();
    }

    private static Entry parse(LocalDateTime time, String user, String action) {
        String[] pieces = action.split(": ", 2);
        String what = pieces[0], name = pieces.length > 1 ? pieces[1] : "";
        if (what.startsWith("Recorded "))
            return new Entry(time, user, "Created", "Created", "Added " + what.substring(9).toLowerCase(Locale.ROOT) + " “" + name + "”");
        if (what.equals("Scheduled payment"))
            return new Entry(time, user, "Created", "Created", "Scheduled payment “" + name + "”");
        if (what.equals("Paid"))
            return new Entry(time, user, "Created", "Paid", "Paid “" + name + "” and recorded it as an expense");
        if (what.equals("Edited transaction"))
            return new Entry(time, user, "Edited", "Edited", "Edited transaction “" + name + "”");
        if (what.equals("Deleted transaction"))
            return new Entry(time, user, "Deleted", "Deleted", "Removed transaction “" + name + "”");
        return new Entry(time, user, "Account", "Account", action.replace(" | ", " · "));
    }

    private JPanel statCards() {
        LocalDate today = LocalDate.now();
        long todayCount = entries.stream().filter(entry -> entry.time.toLocalDate().equals(today)).count();
        long added = entries.stream().filter(entry -> entry.details.startsWith("Added")).count();
        long changes = entries.stream().filter(entry -> entry.kind.equals("Edited") || entry.kind.equals("Deleted")).count();
        JPanel row = Theme.equal(12,
            stat("Actions today", Long.toString(todayCount), null, STYLE_GREEN, null, Color.WHITE, 30),
            stat("Records added", Long.toString(added), null, STYLE_WHITE, null, Theme.INK, 30),
            stat("Edits & deletions", Long.toString(changes), null, STYLE_DARK, null, Color.WHITE, 30));
        row.setPreferredSize(new Dimension(700, 96));
        JPanel holder = Theme.clear(new BorderLayout());
        holder.add(row);
        holder.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return holder;
    }

    @Override protected String subtitle() { return "A record of every change and deletion in StackUp."; }

}

```

## src/frontend/ChartPanel.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** Real-data charts: nothing is drawn unless a saved record backs it. */
public final class ChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public enum Mode { CASH_FLOW, MONTHLY, TOTALS, REVENUE_CATEGORIES, EXPENSE_CATEGORIES }
    private static final Color[] SLICES = {Theme.INK, Theme.BRAND, Theme.BRAND_MID, Theme.BRAND_PALE, new Color(200, 205, 203)};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final List<FinancialRecord> records;
    private final Mode mode;
    private final int year;

    public ChartPanel(List<FinancialRecord> records, Mode mode, int year) {
        this.records = List.copyOf(records);
        this.mode = mode;
        this.year = year;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 240));
    }

    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        if (mode == Mode.REVENUE_CATEGORIES || mode == Mode.EXPENSE_CATEGORIES) donut(pen);
        else if (mode == Mode.CASH_FLOW) cashFlow(pen);
        else if (mode == Mode.MONTHLY) monthly(pen);
        else totals(pen);
        pen.dispose();
    }

    // ------------------------------------------------------------ helpers

    /** Smallest "round" ceiling (1, 2, 2.5, 5 or 10 times a power of ten) above the value. */
    private static double ceiling(double value) {
        if (value <= 0) return 100;
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        for (double step : new double[]{1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10}) {
            if (value <= step * magnitude) return step * magnitude;
        }
        return 10 * magnitude;
    }

    private void text(Graphics2D pen, String text, double x, double y, Color color, boolean center) {
        pen.setColor(color);
        pen.setFont(Theme.fontFor(text, pen.getFont().getStyle(), pen.getFont().getSize2D()));
        double width = pen.getFontMetrics().stringWidth(text);
        pen.drawString(text, (float) (center ? x - width / 2 : x), (float) y);
    }

    private void legend(Graphics2D pen, int right, int y, String[] names, Paint[] paints) {
        pen.setFont(Theme.font(Font.PLAIN, 13));
        int x = right;
        for (int i = names.length - 1; i >= 0; i--) {
            int width = pen.getFontMetrics().stringWidth(names[i]);
            x -= width;
            text(pen, names[i], x, y + 4, Theme.INK, false);
            x -= 18;
            pen.setPaint(paints[i]);
            pen.fill(new RoundRectangle2D.Double(x, y - 5, 12, 12, 5, 5));
            x -= 14;
        }
    }

    /** Vertical bar with rounded corners at the top only. */
    private void bar(Graphics2D pen, Paint paint, double x, double top, double width, double bottom) {
        double height = bottom - top;
        if (height < 1) return;
        double arc = Math.min(Math.min(width, 22), height * 2);
        Shape shape = new RoundRectangle2D.Double(x, top, width, height + arc / 2, arc, arc);
        pen.setPaint(paint);
        Shape previous = pen.getClip();
        pen.clipRect((int) Math.floor(x) - 1, (int) Math.floor(top) - 1, (int) Math.ceil(width) + 2, (int) Math.ceil(height) + 2);
        pen.fill(shape);
        pen.setClip(previous);
    }

    private void grid(Graphics2D pen, int left, int right, int top, int bottom, double max, boolean lines) {
        pen.setFont(Theme.font(Font.PLAIN, 12));
        for (int i = 0; i <= 4; i++) {
            double y = bottom - (bottom - top) * i / 4.0;
            if (lines && i > 0) { pen.setColor(new Color(0, 0, 0, 14)); pen.draw(new Line2D.Double(left, y, right, y)); }
            double value = max * i / 4;
            String label = value == 0 ? "0" : value >= 1000 ? Theme.compact(value).substring(1) : String.format(java.util.Locale.US, "%.0f", value);
            text(pen, label, left - 12 - pen.getFontMetrics().stringWidth(label), y + 4, Theme.MUTED, false);
        }
    }

    private void emptyNote(Graphics2D pen, int cx, int cy) {
        pen.setColor(Theme.MINT);
        pen.fill(new RoundRectangle2D.Double(cx - 125, cy - 28, 250, 56, 28, 28));
        pen.setFont(Theme.font(Font.BOLD, 13));
        text(pen, "Your chart starts with your data", cx, cy - 2, Theme.BRAND, true);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        text(pen, "No transactions recorded yet", cx, cy + 14, Theme.MUTED, true);
    }

    // -------------------------------------------------------------- charts

    /** Dashboard: expenses in black with profit stacked on top in stripes, one bar per month. */
    private void cashFlow(Graphics2D pen) {
        BigDecimal[][] data = ChartData.monthly(records, year);
        double highest = 0;
        for (int m = 0; m < 12; m++)
            highest = Math.max(highest, data[1][m].doubleValue() + Math.max(0, data[2][m].doubleValue()));
        double max = ceiling(highest);
        int left = 46, right = getWidth() - 6, top = 44, bottom = getHeight() - 30;
        legend(pen, right, 14, new String[]{"Profit", "Expenses"}, new Paint[]{Theme.hatch(Theme.BRAND_MID), Theme.INK});
        grid(pen, left, right, top, bottom, max, true);
        double slot = (right - left) / 12.0, width = Math.max(8, Math.min(34, slot - 6));
        Paint stripes = Theme.hatch(Theme.BRAND_MID);
        for (int m = 0; m < 12; m++) {
            double x = left + slot * m + (slot - width) / 2;
            double expense = data[1][m].doubleValue(), profit = Math.max(0, data[2][m].doubleValue());
            double expenseTop = bottom - expense / max * (bottom - top);
            double profitTop = expenseTop - profit / max * (bottom - top);
            bar(pen, Theme.INK, x, expenseTop, width, bottom);
            if (profit > 0) bar(pen, stripes, x, profitTop, width, expenseTop - 2);
            text(pen, slot < 34 ? MONTHS[m].substring(0, 1) : MONTHS[m], x + width / 2, bottom + 20, Theme.MUTED, true);
        }
        if (records.isEmpty()) emptyNote(pen, (left + right) / 2, (top + bottom) / 2);
    }

    /** Insights: revenue (stripes) beside expenses (black) for each month of the year. */
    private void monthly(Graphics2D pen) {
        BigDecimal[][] data = ChartData.monthly(records, year);
        double highest = 0;
        for (int m = 0; m < 12; m++) highest = Math.max(highest, Math.max(data[0][m].doubleValue(), data[1][m].doubleValue()));
        double max = ceiling(highest);
        int left = 52, right = getWidth() - 12, top = 24, bottom = getHeight() - 34;
        grid(pen, left, right, top, bottom, max, true);
        double slot = (right - left) / 12.0, width = Math.max(5, Math.min(24, slot / 3.2));
        Paint stripes = Theme.hatch(Theme.BRAND_MID);
        for (int m = 0; m < 12; m++) {
            double center = left + slot * (m + .5);
            double revenue = data[0][m].doubleValue(), expense = data[1][m].doubleValue();
            double revenueTop = bottom - revenue / max * (bottom - top), expenseTop = bottom - expense / max * (bottom - top);
            if (revenue > 0) bar(pen, stripes, center - width - 1.5, revenueTop, width, bottom);
            else bar(pen, stripes, center - width - 1.5, bottom - 3, width, bottom);
            if (expense > 0) bar(pen, Theme.INK, center + 1.5, expenseTop, width, bottom);
            else bar(pen, Theme.INK, center + 1.5, bottom - 3, width, bottom);
            text(pen, slot < 34 ? MONTHS[m].substring(0, 1) : MONTHS[m], center, bottom + 22, Theme.MUTED, true);
        }
        if (records.isEmpty()) emptyNote(pen, (left + right) / 2, (top + bottom) / 2);
    }

    /** Report: three bars with their values printed above them. */
    private void totals(Graphics2D pen) {
        double revenue = ChartData.total(records, "Revenue").doubleValue();
        double expense = ChartData.total(records, "Expense").doubleValue();
        double[] values = {revenue, expense, revenue - expense};
        String[] names = {"Revenue", "Expenses", "Profit"};
        Paint[] paints = {Theme.hatch(Theme.BRAND_MID), Theme.INK, Theme.BRAND};
        double max = ceiling(Math.max(revenue, expense)), min = values[2] < 0 ? -ceiling(-values[2]) : 0;
        int left = 52, right = getWidth() - 16, top = 34, bottom = getHeight() - 34;
        double zero = bottom - (0 - min) / (max - min) * (bottom - top);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        for (int i = 0; i <= 4; i++) {
            double value = min + (max - min) * i / 4;
            double y = bottom - (bottom - top) * i / 4.0;
            String label = value == 0 ? "0" : Theme.compact(value).replace("₱", "");
            if (i > 0) { pen.setColor(new Color(0, 0, 0, 14)); pen.draw(new Line2D.Double(left, y, right, y)); }
            text(pen, label, left - 12 - pen.getFontMetrics().stringWidth(label), y + 4, Theme.MUTED, false);
        }
        pen.setColor(new Color(0, 0, 0, 40));
        pen.draw(new Line2D.Double(left, zero, right, zero));
        double slot = (right - left) / 3.0, width = Math.min(120, slot * .5);
        for (int i = 0; i < 3; i++) {
            double x = left + slot * i + (slot - width) / 2;
            double y = bottom - (values[i] - min) / (max - min) * (bottom - top);
            if (values[i] >= 0) bar(pen, paints[i], x, y, width, zero);
            else {
                pen.setPaint(paints[i]);
                pen.fill(new RoundRectangle2D.Double(x, zero - 8, width, y - zero + 8, 16, 16));
            }
            pen.setFont(Theme.font(Font.BOLD, 13));
            text(pen, Theme.compact(values[i]), x + width / 2, (values[i] >= 0 ? y : zero) - 10, Theme.INK, true);
            pen.setFont(Theme.font(Font.PLAIN, 13));
            text(pen, names[i], x + width / 2, bottom + 22, Theme.MUTED, true);
        }
        if (records.isEmpty()) emptyNote(pen, (left + right) / 2, (top + bottom) / 2);
    }

    /** Doughnut with the total in the middle and a legend of amounts and shares. */
    private void donut(Graphics2D pen) {
        String type = mode == Mode.REVENUE_CATEGORIES ? "Revenue" : "Expense";
        Map<String, BigDecimal> data = ChartData.categories(records, type);
        BigDecimal total = ChartData.total(records, type);
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(data.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        if (entries.size() > SLICES.length) {
            BigDecimal rest = BigDecimal.ZERO;
            for (int i = SLICES.length - 1; i < entries.size(); i++) rest = rest.add(entries.get(i).getValue());
            entries = new ArrayList<>(entries.subList(0, SLICES.length - 1));
            entries.add(Map.entry("Other", rest));
        }
        int diameter = Math.max(90, Math.min(getHeight() - 8, 150));
        int x = 4, y = (getHeight() - diameter) / 2;
        double hole = diameter * .62;
        if (entries.isEmpty()) {
            Area ring = new Area(new Ellipse2D.Double(x, y, diameter, diameter));
            ring.subtract(new Area(new Ellipse2D.Double(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole)));
            pen.setColor(Theme.SOFT);
            pen.fill(ring);
        } else {
            double angle = 90;
            for (int i = 0; i < entries.size(); i++) {
                double share = entries.get(i).getValue().doubleValue() / total.doubleValue();
                double sweep = -360 * share;
                Area slice = new Area(new Arc2D.Double(x, y, diameter, diameter, angle, sweep, Arc2D.PIE));
                slice.subtract(new Area(new Ellipse2D.Double(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole)));
                pen.setColor(SLICES[i]);
                pen.fill(slice);
                angle += sweep;
            }
        }
        double cx = x + diameter / 2.0, cy = y + diameter / 2.0;
        pen.setFont(Theme.font(Font.PLAIN, 12));
        text(pen, type.equals("Revenue") ? "Revenue" : "Expenses", cx, cy - 4, Theme.MUTED, true);
        pen.setFont(Theme.font(Font.BOLD, 17));
        text(pen, Theme.compact(total.doubleValue()), cx, cy + 17, Theme.INK, true);
        int legendLeft = x + diameter + 30, legendRight = getWidth() - 2;
        int rowHeight = 32, startY = Math.max(16, (getHeight() - rowHeight * Math.max(1, entries.size())) / 2 + 18);
        if (entries.isEmpty()) {
            pen.setFont(Theme.font(Font.PLAIN, 13));
            text(pen, "No records yet", legendLeft, getHeight() / 2.0 + 4, Theme.MUTED, false);
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            int rowY = startY + i * rowHeight;
            pen.setColor(SLICES[i]);
            pen.fill(new RoundRectangle2D.Double(legendLeft, rowY - 9, 13, 13, 5, 5));
            pen.setFont(Theme.font(Font.PLAIN, 14));
            String name = entries.get(i).getKey();
            while (name.length() > 3 && pen.getFontMetrics().stringWidth(name) > legendRight - legendLeft - 150) name = name.substring(0, name.length() - 2) + "…";
            text(pen, name, legendLeft + 24, rowY + 2, Theme.INK, false);
            double share = entries.get(i).getValue().doubleValue() / total.doubleValue() * 100;
            String percent = String.format(java.util.Locale.US, "%.0f%%", share);
            pen.setFont(Theme.font(Font.PLAIN, 13));
            int percentWidth = pen.getFontMetrics().stringWidth(percent);
            text(pen, percent, legendRight - percentWidth, rowY + 2, Theme.MUTED, false);
            pen.setFont(Theme.fontFor("₱", Font.BOLD, 14));
            String amount = Theme.whole(entries.get(i).getValue());
            text(pen, amount, legendRight - percentWidth - 8 - pen.getFontMetrics().stringWidth(amount), rowY + 2, Theme.INK, false);
            if (i < entries.size() - 1) {
                pen.setColor(Theme.LINE);
                pen.draw(new Line2D.Double(legendLeft, rowY + 17, legendRight, rowY + 17));
            }
        }
    }
}

```

## src/frontend/DashboardPage.java

```java
package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Overview: this month's figures, the payments still to be made and the latest transactions. */
public final class DashboardPage extends PageSupport {
    private static final long serialVersionUID = 1L;
    private final List<FinancialRecord> records = service.getRecords();

    public DashboardPage(DashboardUI window) {
        super(window);
        JPanel stats = monthCards();
        stats.setPreferredSize(new Dimension(700, 140));
        add(stats, BorderLayout.NORTH);
        add(Theme.columns(12, new double[]{2.6, 3.4}, pendingCard(), recentCard()));
    }

    @Override protected String subtitle() { return "Stay on top of your revenue, expenses and upcoming payments."; }

    /** The month label, plus quick Add buttons for employees (admins only view). */
    @Override protected JComponent actions() {
        JPanel row = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        JButton month = Theme.outline(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
        month.setIcon(Icons.of(Icons.Kind.CALENDAR, Theme.INK, 18));
        month.setFocusable(false);
        month.setCursor(Cursor.getDefaultCursor());
        row.add(month);
        if (!admin) {
            JButton addRevenue = Theme.dark("Add Revenue");
            addRevenue.setIcon(Icons.of(Icons.Kind.PLUS, Color.WHITE, 16));
            addRevenue.addActionListener(event -> record("Revenue", null));
            JButton addExpense = Theme.outline("Add Expense");
            addExpense.setIcon(Icons.of(Icons.Kind.MINUS, Theme.INK, 16));
            addExpense.addActionListener(event -> record("Expense", null));
            row.add(addRevenue);
            row.add(addExpense);
        }
        return row;
    }

    /** Revenue, expenses and profit for this month, each compared with last month. */
    private JPanel monthCards() {
        LocalDate start = monthStart(), end = monthEnd(), previousStart = start.minusMonths(1), previousEnd = start.minusDays(1);
        BigDecimal revenue = totalIn("Revenue", start, end), expense = totalIn("Expense", start, end);
        BigDecimal lastRevenue = totalIn("Revenue", previousStart, previousEnd), lastExpense = totalIn("Expense", previousStart, previousEnd);
        BigDecimal profit = revenue.subtract(expense), lastProfit = lastRevenue.subtract(lastExpense);

        JPanel revenueCard = stat("Total Revenue", Theme.whole(revenue), null, STYLE_GREEN, Icons.Kind.TREND, Color.WHITE, 28);
        addFoot(revenueCard, change(revenue, lastRevenue, true, "This month", true));
        JPanel expenseCard = stat("Total Expenses", Theme.whole(expense), null, STYLE_DARK, Icons.Kind.WALLET, Color.WHITE, 28);
        addFoot(expenseCard, change(expense, lastExpense, true, "This month", false));
        JPanel profitCard = stat("Profit", Theme.whole(profit), null, STYLE_WHITE, Icons.Kind.BARS, Theme.INK, 28);
        addFoot(profitCard, change(profit, lastProfit, false, "This month", true));
        return Theme.equal(12, revenueCard, expenseCard, profitCard);
    }

    private List<Payment> pending() {
        List<Payment> list = new ArrayList<>();
        for (Payment payment : service.getPayments()) if (!payment.isPaid()) list.add(payment);
        list.sort(Comparator.comparing(Payment::getDue));
        return list;
    }

    /** Pending payments: the total owed and a reminder list sorted by due date. */
    private JPanel pendingCard() {
        List<Payment> pending = pending();
        BigDecimal total = BigDecimal.ZERO;
        long dueSoon = 0;
        for (Payment payment : pending) {
            total = total.add(payment.getAmount());
            if (ChronoUnit.DAYS.between(LocalDate.now(), payment.getDue()) <= 10) dueSoon++;
        }

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        JPanel title = Theme.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        title.add(Theme.badge(Icons.Kind.CLOCK, Theme.SOFT, Theme.INK, 38));
        title.add(Theme.label("Pending Payments", 20, Theme.INK));
        head.add(title, BorderLayout.WEST);
        if (!admin) {
            JButton schedule = Theme.outline("+ Schedule");
            schedule.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            schedule.addActionListener(event -> schedule());
            head.add(schedule, BorderLayout.EAST);
        }

        JPanel summary = Theme.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        summary.add(Theme.label(Theme.whole(total), 28, Theme.INK));
        summary.add(pill(dueSoon + " due in 10 days", null, Theme.SOFT, Theme.INK));
        JPanel top = Theme.clear(new BorderLayout(0, 12));
        top.add(head, BorderLayout.NORTH);
        top.add(summary, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);

        DefaultTableModel rows = model("Reminder", "Amount", "Status");
        for (Payment payment : pending.subList(0, Math.min(6, pending.size())))
            rows.addRow(new Object[]{new Tile(payment.getDue(), payment.getDescription()),
                Theme.money(payment.getAmount()), dueText(payment)});
        JTable table = table(rows, "tile", "bold", "chip");
        table.setRowHeight(52);
        widths(table, 230, 110, 175);
        card.add(tableArea(table, "No pending payments"));
        if (pending.size() > 6) card.add(Theme.muted("+" + (pending.size() - 6) + " more scheduled", 13), BorderLayout.SOUTH);
        return card;
    }

    private JPanel recentCard() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Theme.label("Recent Transactions", 20, Theme.INK), BorderLayout.NORTH);
        DefaultTableModel rows = model("Description", "Amount", "Type", "Recorded By", "Date");
        List<FinancialRecord> latest = new ArrayList<>(records);
        Collections.reverse(latest);
        latest.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        for (FinancialRecord r : latest.subList(0, Math.min(6, latest.size())))
            rows.addRow(new Object[]{new Desc(r.getDescription(), r.getCategory()), signed(r), r.getType(), r.getRecordedBy(), r.getDate().format(STAMP)});
        JTable table = table(rows, "desc", "money", "chip", "user", "muted");
        table.setRowHeight(52);
        widths(table, 200, 120, 110, 120, 170);
        card.add(tableArea(table, "No transactions recorded yet."));
        JButton all = Theme.link("View all transactions →");
        all.addActionListener(event -> window.navigate("Transaction History"));
        JPanel foot = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        foot.add(all);
        card.add(foot, BorderLayout.SOUTH);
        return card;
    }
}

```

## src/frontend/DashboardUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Desktop shell: brand and account pills on top, a slim icon rail on the left, the page beside it. */
public final class DashboardUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private final FinancialService service;
    private final boolean admin;
    private final JPanel workspace = Theme.clear(new BorderLayout(0, 10));
    private final Map<String, RailButton> menu = new LinkedHashMap<>();
    private String current = "Dashboard";

    public DashboardUI(FinancialService service) {
        super("StackUp Financial Management System");
        this.service = service;
        admin = service.getCurrent().getRole().equals("Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        java.awt.image.BufferedImage icon = loadLogo();
        if (icon != null) setIconImage(icon);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1500, screen.width * 97 / 100), screen.height);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.FRAME);
        outer.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(outer);
        Theme.RoundedPanel root = new Theme.RoundedPanel(Theme.BACKGROUND);
        root.radius = 38;
        root.setLayout(new BorderLayout(14, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        outer.add(root);

        root.add(topBar(), BorderLayout.NORTH);
        root.add(rail(), BorderLayout.WEST);
        root.add(workspace);
        showPage("Dashboard");
    }

    public FinancialService service() { return service; }
    public void refresh() { showPage(current); }
    public void navigate(String page) { open(page); }

    private String[] pages() {
        return admin
            ? new String[]{"Dashboard", "Transaction History", "Financial Report", "Financial Insights", "Activity Log", "Accounts"}
            : new String[]{"Dashboard", "Financial Record", "Payments", "Transaction History"};
    }

    private static Icons.Kind iconFor(String page) {
        switch (page) {
            case "Financial Record": return Icons.Kind.PLUS_CIRCLE;
            case "Payments": return Icons.Kind.CLOCK;
            case "Transaction History": return Icons.Kind.DOC;
            case "Financial Report": return Icons.Kind.BARS;
            case "Financial Insights": return Icons.Kind.PIE;
            case "Activity Log": return Icons.Kind.PULSE;
            case "Accounts": return Icons.Kind.USERS;
            default: return Icons.Kind.HOME;
        }
    }

    // ------------------------------------------------------------ top bar

    private static java.awt.image.BufferedImage loadLogo() {
        try { return javax.imageio.ImageIO.read(Theme.asset("logo.png")); }
        catch (Exception exception) { return null; }
    }

    private JComponent topBar() {
        JPanel bar = Theme.clear(new BorderLayout());
        Theme.RoundedPanel brand = new Theme.RoundedPanel(Color.BLACK);
        brand.radius = 999;
        brand.setLayout(new BorderLayout());
        brand.add(new JComponent() {
            private static final long serialVersionUID = 1L;
            private final java.awt.image.BufferedImage logo = loadLogo();
            public Dimension getPreferredSize() {
                if (logo == null) return new Dimension(120, 46);
                return new Dimension(logo.getWidth() * 46 / logo.getHeight(), 46);
            }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                pen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (logo != null) pen.drawImage(logo, 0, 0, getWidth(), getHeight(), null);
                pen.dispose();
            }
        });
        brand.setBorder(new EmptyBorder(0, 26, 0, 26));
        bar.add(brand, BorderLayout.WEST);

        JPanel right = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        Theme.RoundedPanel tools = pill();
        tools.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 6));
        JButton bell = toolButton(Icons.Kind.BELL, "Payment reminders", event -> remind());
        tools.add(bell);
        right.add(tools);
        right.add(userPill());
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private Theme.RoundedPanel pill() {
        Theme.RoundedPanel panel = new Theme.RoundedPanel(Color.WHITE);
        panel.radius = 999;
        return panel;
    }

    private JButton toolButton(Icons.Kind kind, String tip, java.awt.event.ActionListener action) {
        JButton button = new JButton(Icons.of(kind, Theme.INK, 20));
        button.setToolTipText(tip);
        button.setBackground(Color.WHITE);
        button.setForeground(Theme.INK);
        button.putClientProperty("ghost", Boolean.TRUE);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 11, 9, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(action);
        return button;
    }

    private void remind() {
        long pending = service.getPayments().stream().filter(p -> !p.isPaid()).count();
        String message;
        if (pending == 0) message = "No payments are waiting.";
        else {
            Payment next = service.getPayments().stream().filter(p -> !p.isPaid())
                .min(java.util.Comparator.comparing(Payment::getDue)).orElseThrow();
            message = pending + " payment(s) pending.\nNext: " + next.getDescription() + " due " + next.getDue().format(PageSupport.DAY) + ".";
        }
        Theme.message(this, "Payment reminders", message);
    }

    private JComponent userPill() {
        Theme.RoundedPanel pill = pill();
        pill.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        Account account = service.getCurrent();
        pill.add(new Theme.Avatar(account.getUsername(), false, 34));
        JPanel text = Theme.clear(new GridLayout(2, 1, 0, 0));
        JLabel who = Theme.label(account.getUsername(), 14, Theme.INK), what = Theme.muted(account.getRole(), 12);
        text.add(who);
        text.add(what);
        text.setPreferredSize(new Dimension(Math.max(who.getPreferredSize().width, what.getPreferredSize().width) + 14, 36));
        pill.add(text);
        pill.setBorder(new EmptyBorder(0, 0, 0, 8));
        return pill;
    }

    private void logout() {
        if (Theme.confirm(this, Icons.Kind.LOGOUT, "Log out?", "You will need to sign in again to keep using StackUp.", "Yes, log out", "Stay signed in")) {
            service.logout();
            new LoginUI(service).setVisible(true);
            dispose();
        }
    }

    // --------------------------------------------------------------- rail

    private JComponent rail() {
        JPanel column = Theme.clear(new BorderLayout());
        Theme.RoundedPanel navigation = pill();
        navigation.radius = 36;
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(new EmptyBorder(6, 6, 6, 6));
        for (String page : pages()) {
            RailButton button = new RailButton(iconFor(page), page);
            button.addActionListener(event -> open(page));
            menu.put(page, button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(2));
        }
        navigation.remove(navigation.getComponentCount() - 1);
        Theme.RoundedPanel help = pill();
        help.radius = 36;
        help.setLayout(new BoxLayout(help, BoxLayout.Y_AXIS));
        help.setBorder(new EmptyBorder(6, 6, 6, 6));
        RailButton logout = new RailButton(Icons.Kind.LOGOUT, "Logout");
        logout.addActionListener(event -> logout());
        help.add(logout);
        JPanel top = Theme.clear(new BorderLayout(0, 10));
        top.add(navigation, BorderLayout.NORTH);
        top.add(help, BorderLayout.CENTER);
        JPanel holder = Theme.clear(new BorderLayout());
        holder.add(top, BorderLayout.NORTH);
        column.add(holder, BorderLayout.NORTH);
        return column;
    }

    private static final class RailButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Icons.Kind kind;
        private boolean active;
        RailButton(Icons.Kind kind, String name) {
            this.kind = kind;
            setName(name);
            setToolTipText(name);
            setPreferredSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        void setActive(boolean value) { active = value; repaint(); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) { pen.setColor(Theme.INK); pen.fillOval(0, 0, getWidth() - 1, getHeight() - 1); }
            else if (getModel().isRollover()) { pen.setColor(Theme.SOFT); pen.fillOval(0, 0, getWidth() - 1, getHeight() - 1); }
            Icon icon = Icons.of(kind, active ? Color.WHITE : Theme.INK, 20);
            icon.paintIcon(this, pen, (getWidth() - 20) / 2, (getHeight() - 20) / 2);
            pen.dispose();
        }
    }

    // --------------------------------------------------------------- pages

    private void open(String page) {
        if (current.equals(page))
            Theme.alreadyOpen(this, page);
        else showPage(page);
    }

    private String greeting() {
        int hour = java.time.LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        return part + ", " + service.getCurrent().getUsername();
    }

    private void showPage(String page) {
        current = page;
        workspace.removeAll();
        for (Map.Entry<String, RailButton> entry : menu.entrySet()) entry.getValue().setActive(entry.getKey().equals(page));
        PageSupport screen;
        switch (page) {
            case "Financial Record": screen = new FinancialRecordUI(this); break;
            case "Transaction History": screen = new TransactionHistoryUI(this); break;
            case "Financial Report": screen = new FinancialReportUI(this); break;
            case "Financial Insights": screen = new FinancialInsightsUI(this); break;
            case "Activity Log": screen = new ActivityLogUI(this); break;
            case "Accounts": screen = new AccountsUI(this); break;
            case "Payments": screen = new PaymentsUI(this); break;
            default: screen = new DashboardPage(this);
        }
        String title = screen.title() != null ? screen.title() : page.equals("Dashboard") ? greeting() : page;
        JPanel header = Theme.clear(new BorderLayout(16, 0));
        JPanel words = Theme.clear(new BorderLayout(0, 4));
        words.add(Theme.label(title, 30, Theme.INK), BorderLayout.NORTH);
        words.add(Theme.plain(screen.subtitle(), 14, Theme.MUTED));
        header.add(words, BorderLayout.WEST);
        JComponent actions = screen.actions();
        if (actions != null) {
            JPanel holder = Theme.clear(new GridBagLayout());
            holder.add(actions);
            header.add(holder, BorderLayout.EAST);
        }
        workspace.add(header, BorderLayout.NORTH);
        workspace.add(scroller(screen));
        workspace.revalidate();
        workspace.repaint();
    }

    private JComponent scroller(JComponent screen) {
        JPanel body = new ScrollBody();
        body.add(screen);
        JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    /** Fills the viewport width, and its height too when the page is shorter than the window. */
    private static final class ScrollBody extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        ScrollBody() { super(new BorderLayout()); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle area, int orientation, int direction) { return 24; }
        public int getScrollableBlockIncrement(Rectangle area, int orientation, int direction) { return Math.max(60, area.height - 60); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }
}

```

## src/frontend/DatePicker.java

```java
package frontend;

import frontend.style.*;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;

/** Read-only rounded input backed by a Swing calendar. */
public final class DatePicker extends Theme.RoundedField {
    private static final long serialVersionUID = 1L;
    private LocalDate selected;
    private YearMonth month = YearMonth.now();

    public DatePicker() {
        setEditable(false);
        setPlaceholder("Select date");
        setToolTipText("Choose a date");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent event) { open(); }
        });
        getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "choose");
        getActionMap().put("choose", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(java.awt.event.ActionEvent event) { open(); }
        });
    }

    public LocalDate getSelectedDate() {
        if (selected == null) throw new IllegalArgumentException("Choose a date.");
        return selected;
    }

    public void setSelectedDate(LocalDate date) {
        selected = date;
        setText(date == null ? "" : date.format(PageSupport.DAY));
        if (date != null) month = YearMonth.from(date);
    }

    private void open() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        JPanel calendar = new JPanel();
        calendar.setBackground(Color.WHITE);
        calendar.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        popup.add(calendar);
        rebuild(calendar, popup);
        popup.show(this, 0, getHeight() + 4);
    }

    private void rebuild(JPanel calendar, JPopupMenu popup) {
        calendar.removeAll();
        calendar.setLayout(new BorderLayout(0, 10));
        JPanel header = Theme.clear(new BorderLayout());
        JButton previous = Theme.outline("‹"), next = Theme.outline("›");
        for (JButton arrow : new JButton[]{previous, next}) {
            arrow.setBorder(BorderFactory.createEmptyBorder());
            arrow.setPreferredSize(new Dimension(32, 32));
        }
        header.add(previous, BorderLayout.WEST);
        JLabel title = Theme.label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), 14, Theme.INK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(title);
        header.add(next, BorderLayout.EAST);
        previous.addActionListener(event -> { month = month.minusMonths(1); rebuild(calendar, popup); });
        next.addActionListener(event -> { month = month.plusMonths(1); rebuild(calendar, popup); });
        calendar.add(header, BorderLayout.NORTH);
        JPanel days = Theme.clear(new GridLayout(7, 7, 2, 2));
        for (String name : new String[]{"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"}) {
            JLabel label = Theme.muted(name, 12);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            days.add(label);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int cell = 0; cell < 42; cell++) {
            int number = cell - offset + 1;
            if (number < 1 || number > month.lengthOfMonth()) { days.add(new JLabel()); continue; }
            LocalDate date = month.atDay(number);
            boolean chosen = date.equals(selected);
            JButton day = chosen ? Theme.solid(Integer.toString(number), Theme.BRAND, Color.WHITE)
                : Theme.link(Integer.toString(number));
            if (!chosen) day.setForeground(date.equals(LocalDate.now()) ? Theme.BRAND : Theme.INK);
            day.setBorder(BorderFactory.createEmptyBorder());
            day.setPreferredSize(new Dimension(36, 32));
            day.addActionListener(event -> { setSelectedDate(date); popup.setVisible(false); });
            days.add(day);
        }
        calendar.add(days);
        calendar.revalidate();
        calendar.repaint();
        popup.pack();
    }
}

```

## src/frontend/Dialogs.java

```java
package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import javax.swing.*;

/** The two pop-up forms: add or edit a record, and schedule a payment. */
public final class Dialogs {
    private Dialogs() {}

    /** Shows the record form. Returns true when a record was saved. */
    public static boolean record(Component owner, FinancialService service, String type, FinancialRecord existing) {
        JTextField description = Theme.input("e.g. Catering order");
        description.setText(existing == null ? "" : existing.getDescription());
        JTextField category = Theme.input(type.equals("Revenue") ? "e.g. Sales" : "e.g. Utilities");
        category.setText(existing == null ? "" : existing.getCategory());
        JTextField amount = Theme.input("0.00");
        amount.setText(existing == null ? "" : existing.getAmount().toPlainString());
        DatePicker date = new DatePicker();
        date.setSelectedDate(existing == null ? LocalDate.now() : existing.getDate().toLocalDate());

        JPanel form = Theme.clear(new GridLayout(4, 1, 0, 14));
        form.add(Theme.field("Description", description));
        form.add(Theme.field("Category", category));
        form.add(Theme.field("Amount (₱)", amount));
        form.add(Theme.field("Date", date));
        form.setPreferredSize(new Dimension(420, 360));

        String title = (existing == null ? "Add " : "Edit ") + type;
        while (JOptionPane.showConfirmDialog(owner, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                LocalTime clock = existing == null ? LocalTime.now().withNano(0) : existing.getDate().toLocalTime();
                LocalDateTime time = date.getSelectedDate().atTime(clock);
                BigDecimal value = new BigDecimal(amount.getText().trim());
                String author = existing == null ? service.getCurrent().getUsername() : existing.getRecordedBy();
                FinancialRecord record = type.equals("Revenue")
                    ? new Revenue(description.getText(), category.getText(), value, time, author)
                    : new Expense(description.getText(), category.getText(), value, time, author);
                if (existing == null) service.addRecord(record); else service.editRecord(existing.getId(), record);
                return true;
            } catch (Exception exception) {
                Theme.message(owner, "Check input", exception.getMessage());
            }
        }
        return false;
    }

    /** Shows the payment form. Returns true when a payment was scheduled. */
    public static boolean payment(Component owner, FinancialService service) {
        JTextField description = Theme.input("e.g. Office rent");
        JTextField category = Theme.input("e.g. Rent");
        JTextField amount = Theme.input("0.00");
        DatePicker date = new DatePicker();

        JPanel form = Theme.clear(new GridLayout(4, 1, 0, 14));
        form.add(Theme.field("Description", description));
        form.add(Theme.field("Category", category));
        form.add(Theme.field("Amount (₱)", amount));
        form.add(Theme.field("Due date", date));
        form.setPreferredSize(new Dimension(420, 360));

        while (JOptionPane.showConfirmDialog(owner, form, "Schedule Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                BigDecimal value = new BigDecimal(amount.getText().trim());
                String by = service.getCurrent().getUsername();
                service.schedule(new Payment(description.getText(), category.getText(), value, date.getSelectedDate(), by));
                return true;
            } catch (Exception exception) {
                Theme.message(owner, "Check input", exception.getMessage());
            }
        }
        return false;
    }
}

```

## src/frontend/FinancialInsightsUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Where the money comes from and goes, with highlights for the chosen period. */
public final class FinancialInsightsUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private final JPanel body = Theme.clear(new BorderLayout(0, 12));
    private final JComponent range;
    private final int year = LocalDate.now().getYear();

    public FinancialInsightsUI(DashboardUI window) {
        super(window);
        range = Theme.segmented(new String[]{"This month", "Quarter", Integer.toString(year)}, 2, Theme.INK, index -> build(index));
        add(body);
        build(2);
    }

    @Override protected String subtitle() { return "Where the money comes from, where it goes, and how each month compares."; }
    @Override protected JComponent actions() { return range; }

    private void build(int choice) {
        LocalDate today = LocalDate.now();
        LocalDate start = choice == 0 ? monthStart()
            : choice == 1 ? LocalDate.of(year, (today.getMonthValue() - 1) / 3 * 3 + 1, 1) : LocalDate.of(year, 1, 1);
        LocalDate end = choice == 0 ? monthEnd() : choice == 1 ? start.plusMonths(3).minusDays(1) : LocalDate.of(year, 12, 31);
        List<FinancialRecord> all = service.getRecords();
        List<FinancialRecord> scope = ChartData.between(all, start, end);

        body.removeAll();
        JPanel top = Theme.clear(new BorderLayout(0, 12));
        top.add(Theme.equal(12, highlight("Revenue", scope, true), highlight("Expense", scope, false)), BorderLayout.NORTH);
        body.add(top, BorderLayout.NORTH);

        JPanel middle = Theme.clear(new BorderLayout(0, 12));
        middle.add(Theme.equal(12,
            chartCard("Revenue by Category", null, new ChartPanel(scope, ChartPanel.Mode.REVENUE_CATEGORIES, year), 170),
            chartCard("Expense by Category", null, new ChartPanel(scope, ChartPanel.Mode.EXPENSE_CATEGORIES, year), 170)), BorderLayout.NORTH);
        JPanel monthly = chartCard("Monthly Performance",
            "January – December " + year + " · " + today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " to date",
            new ChartPanel(all, ChartPanel.Mode.MONTHLY, year), 150);
        JPanel legend = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.add(legendItem("Revenue", Theme.hatch(Theme.BRAND_MID)));
        legend.add(legendItem("Expenses", Theme.INK));
        ((JPanel) monthly.getComponent(0)).add(legend, BorderLayout.EAST);
        middle.add(monthly, BorderLayout.CENTER);
        body.add(middle);
        body.revalidate();
        body.repaint();
    }

    private JComponent legendItem(String name, Paint paint) {
        JLabel label = Theme.plain(name, 13, Theme.INK);
        label.setIcon(new Icon() {
            public int getIconWidth() { return 14; }
            public int getIconHeight() { return 14; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setPaint(paint);
                pen.fillRoundRect(x, y, 14, 14, 6, 6);
                pen.dispose();
            }
        });
        label.setIconTextGap(6);
        return label;
    }

    private JPanel chartCard(String title, String subtitle, JComponent chart, int height) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        JPanel words = Theme.clear(new GridLayout(subtitle == null ? 1 : 2, 1, 0, 2));
        words.add(Theme.label(title, 22, Theme.INK));
        if (subtitle != null) words.add(Theme.muted(subtitle, 14));
        head.add(words, BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(300, height));
        if (subtitle != null) {
            Theme.RoundedPanel plot = new Theme.RoundedPanel(Theme.BACKGROUND);
            plot.radius = 22;
            plot.setLayout(new BorderLayout());
            plot.setBorder(new EmptyBorder(6, 10, 4, 10));
            plot.add(chart);
            card.add(plot);
        } else card.add(chart);
        return card;
    }

    private JPanel highlight(String type, List<FinancialRecord> scope, boolean revenue) {
        FinancialRecord best = null;
        for (FinancialRecord record : scope)
            if (record.getType().equals(type) && (best == null || record.getAmount().compareTo(best.getAmount()) > 0)) best = record;
        Theme.RoundedPanel card = Theme.hero(revenue ? Theme.BRAND : Theme.INK);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(new EmptyBorder(14, 20, 14, 20));
        card.add(Theme.badge(revenue ? Icons.Kind.UP_RIGHT : Icons.Kind.DOWN_RIGHT, Color.WHITE, revenue ? Theme.BRAND : Theme.INK, 52), BorderLayout.WEST);
        JPanel text = Theme.clear(new GridLayout(2, 1, 0, 3));
        text.add(Theme.plain("Highest " + type, 15, new Color(232, 244, 238)));
        text.add(Theme.label(best == null ? "—" : Theme.money(best.getAmount()), 32, Color.WHITE));
        card.add(text);
        return card;
    }
}

```

## src/frontend/FinancialRecordUI.java

```java
package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Employee data entry: a form on the left, your own latest records on the right. */
public final class FinancialRecordUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private static final String[] REVENUE = {"Sales", "Services", "Other income"};
    private static final String[] EXPENSE = {"Rent", "Utilities", "Payroll", "Supplies", "Marketing", "Other"};
    private String type = "Revenue";
    private final List<FinancialRecord> mine = new ArrayList<>();

    public FinancialRecordUI(DashboardUI window) {
        super(window);
        String me = service.getCurrent().getUsername();
        for (FinancialRecord r : service.getRecords()) if (me.equalsIgnoreCase(r.getRecordedBy())) mine.add(r);
        add(Theme.columns(12, new double[]{2.2, 4.4}, form(), right()));
    }

    @Override protected String subtitle() { return "Log a new revenue or expense — it is saved to your name and shows in reports right away."; }

    /** Revenue / Expense choice as two big buttons: the chosen one is filled, the other outlined. */
    private JPanel typeButtons(Runnable onChange) {
        JButton revenue = Theme.button("Revenue"), expense = Theme.button("Expense");
        for (JButton button : new JButton[]{revenue, expense}) button.setPreferredSize(new Dimension(100, 54));
        Runnable paint = () -> {
            boolean isRevenue = type.equals("Revenue");
            style(revenue, isRevenue, Theme.BRAND, Icons.Kind.PLUS);
            style(expense, !isRevenue, Theme.INK, Icons.Kind.MINUS);
        };
        revenue.addActionListener(event -> { type = "Revenue"; paint.run(); onChange.run(); });
        expense.addActionListener(event -> { type = "Expense"; paint.run(); onChange.run(); });
        paint.run();
        return Theme.equal(10, revenue, expense);
    }

    private static void style(JButton button, boolean chosen, Color fill, Icons.Kind icon) {
        button.putClientProperty("outline", chosen ? null : Boolean.TRUE);
        button.setBackground(chosen ? fill : Color.WHITE);
        button.setForeground(chosen ? Color.WHITE : Theme.INK);
        button.setIcon(Icons.of(icon, chosen ? Color.WHITE : Theme.INK, 16));
        button.repaint();
    }

    private JPanel form() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(Theme.label("New Record", 20, Theme.INK), BorderLayout.NORTH);

        JComboBox<String> category = new JComboBox<>(REVENUE);
        JTextField description = Theme.input("e.g. Catering order");
        JTextField amount = Theme.input("0.00");
        DatePicker date = new DatePicker();
        date.setSelectedDate(LocalDate.now());
        JButton save = Theme.button("Save Revenue");
        save.setPreferredSize(new Dimension(100, 50));
        JButton clear = Theme.outline("Clear");
        clear.setPreferredSize(new Dimension(100, 50));

        JPanel kind = typeButtons(() -> {
            category.setModel(new DefaultComboBoxModel<>(type.equals("Revenue") ? REVENUE : EXPENSE));
            save.setText("Save " + type);
        });

        JPanel fields = Theme.clear(new GridLayout(4, 1, 0, 14));
        fields.add(Theme.field("Type", kind));
        fields.add(Theme.field("Description", description));
        fields.add(Theme.equal(12, Theme.field("Category", category), Theme.field("Amount (₱)", amount)));
        fields.add(Theme.field("Date", date));
        for (JComponent input : new JComponent[]{description, amount, date, category}) input.setPreferredSize(new Dimension(150, 48));
        JPanel top = Theme.clear(new BorderLayout());
        top.add(fields, BorderLayout.NORTH);
        card.add(top);

        clear.addActionListener(event -> { description.setText(""); amount.setText(""); date.setSelectedDate(LocalDate.now()); });
        save.addActionListener(event -> {
            try {
                LocalDateTime time = date.getSelectedDate().atTime(LocalTime.now().withNano(0));
                BigDecimal value = new BigDecimal(amount.getText().trim());
                String author = service.getCurrent().getUsername();
                String chosen = (String) category.getSelectedItem();
                service.addRecord(type.equals("Revenue")
                    ? new Revenue(description.getText(), chosen, value, time, author)
                    : new Expense(description.getText(), chosen, value, time, author));
                window.refresh();
            } catch (NumberFormatException exception) {
                Theme.message(this, "Check input", "Enter a positive amount with up to two decimal places.");
            } catch (Exception exception) {
                error(exception);
            }
        });
        card.add(Theme.equal(12, clear, save), BorderLayout.SOUTH);
        return card;
    }

    /** Your latest records. */
    private JPanel right() {

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.label("My Recent Records", 20, Theme.INK), BorderLayout.WEST);
        JButton all = Theme.link("View all →");
        all.addActionListener(event -> window.navigate("Transaction History"));
        head.add(all, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        DefaultTableModel rows = model("Description", "Type", "Amount", "Date");
        List<FinancialRecord> latest = new ArrayList<>(mine);
        Collections.reverse(latest);
        latest.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        for (FinancialRecord r : latest.subList(0, Math.min(8, latest.size())))
            rows.addRow(new Object[]{new Desc(r.getDescription(), r.getCategory()), r.getType(), signed(r), r.getDate().toLocalDate().format(DAY)});
        JTable table = table(rows, "desc", "chip", "money", "muted");
        table.setRowHeight(56);
        widths(table, 200, 120, 130, 110);
        card.add(tableArea(table, "You haven't recorded anything yet."));

        return card;
    }
}

```

## src/frontend/FinancialReportUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Revenue, expenses and profit for any date range, with charts and a PDF download. */
public final class FinancialReportUI extends PageSupport {
    private static final long serialVersionUID = 1L;

    public FinancialReportUI(DashboardUI window) {
        super(window);
        DatePicker from = new DatePicker(), to = new DatePicker();
        from.setSelectedDate(monthStart());
        to.setSelectedDate(monthEnd());
        JButton generate = Theme.button("Generate Report");
        generate.setPreferredSize(new Dimension(190, 44));
        JPanel filters = Theme.card();
        filters.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.insets = new Insets(0, 0, 0, 16);
        c.gridx = 0; filters.add(Theme.field("From date", from), c);
        c.gridx = 1; filters.add(Theme.field("To date", to), c);
        c.gridx = 2; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0); c.anchor = GridBagConstraints.SOUTH;
        filters.add(generate, c);
        JPanel body = Theme.clear(new BorderLayout(0, 12));
        JPanel top = Theme.clear(new BorderLayout(0, 12));
        top.add(filters, BorderLayout.NORTH);
        body.add(top, BorderLayout.NORTH);
        JPanel charts = Theme.clear(new BorderLayout());
        body.add(charts);
        add(body);

        Runnable update = () -> {
            try {
                LocalDate start = from.getSelectedDate(), end = to.getSelectedDate();
                if (start.isAfter(end)) throw new IllegalArgumentException("From date must be before To date.");
                List<FinancialRecord> records = ChartData.between(service.getRecords(), start, end);
                charts.removeAll();
                charts.add(chartCard("Revenue vs Expenses vs Profit",
                    new ChartPanel(records, ChartPanel.Mode.TOTALS, start.getYear())));
                body.revalidate();
                body.repaint();
            } catch (Exception exception) { error(exception); }
        };
        generate.addActionListener(event -> update.run());
        update.run();
    }

    @Override protected String subtitle() { return "Revenue, expenses and profit for any date range."; }

    private JPanel chartCard(String title, JComponent chart) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(Theme.label(title, 22, Theme.INK), BorderLayout.NORTH);
        Theme.RoundedPanel plot = new Theme.RoundedPanel(Theme.BACKGROUND);
        plot.radius = 22;
        plot.setLayout(new BorderLayout());
        plot.setBorder(new EmptyBorder(8, 10, 6, 10));
        plot.add(chart);
        chart.setPreferredSize(new Dimension(300, 300));
        card.add(plot);
        return card;
    }



    private static List<Map.Entry<String, BigDecimal>> ranked(Map<String, BigDecimal> data) {
        List<Map.Entry<String, BigDecimal>> list = new ArrayList<>(data.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return list;
    }
}

```

## src/frontend/LoginUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Just a username, a password and one button. */
public final class LoginUI extends JFrame {
    private static final long serialVersionUID = 1L;

    public LoginUI(FinancialService service) {
        super("StackUp Financial Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try { setIconImage(javax.imageio.ImageIO.read(Theme.asset("logo.png"))); } catch (Exception ignored) { }
        setSize(540, 660);
        setMinimumSize(new Dimension(500, 620));
        setLocationRelativeTo(null);
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.FRAME);
        setContentPane(outer);

        boolean first = !service.hasAccounts();
        Theme.RoundedPanel card = new Theme.RoundedPanel(Color.WHITE);
        card.radius = 34;
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 34, 40));
        card.setPreferredSize(new Dimension(440, 540));
        JTextField username = Theme.input("e.g. maria");
        Theme.RoundedPassword password = new Theme.RoundedPassword();
        password.setPlaceholder(first ? "At least 8 characters" : "Your password");
        JButton login = Theme.button(first ? "Create Account" : "Log In");
        login.setPreferredSize(new Dimension(100, 48));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0; c.insets = new Insets(0, 0, 28, 0); card.add(new LogoPanel(), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 0, 0); card.add(Theme.field("Username", username), c);
        c.gridy = 2; c.insets = new Insets(18, 0, 0, 0); card.add(Theme.field("Password", password), c);
        c.gridy = 3; c.insets = new Insets(30, 0, 0, 0); card.add(login, c);
        outer.add(card);

        login.addActionListener(event -> {
            char[] value = password.getPassword();
            try {
                if (first) service.createAccount(username.getText(), value, "Admin");
                service.login(username.getText(), value);
                new DashboardUI(service).setVisible(true);
                dispose();
            } catch (Exception exception) {
                Theme.message(this, "Cannot sign in", exception.getMessage());
            } finally {
                Arrays.fill(value, '\0');
                password.setText("");
            }
        });
        getRootPane().setDefaultButton(login);
    }

    /** The StackUp logo. Its white lettering is recoloured dark so it reads on the white card. */
    private static final class LogoPanel extends JComponent {
        private static final long serialVersionUID = 1L;
        private final java.awt.image.BufferedImage logo;
        LogoPanel() {
            java.awt.image.BufferedImage image = null;
            try { image = javax.imageio.ImageIO.read(Theme.asset("logo.png")); }
            catch (Exception ignored) { }
            if (image != null) {
                for (int y = 0; y < image.getHeight(); y++)
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        if ((argb >>> 24) > 0 && ((argb >> 16) & 255) > 185 && ((argb >> 8) & 255) > 185 && (argb & 255) > 185)
                            image.setRGB(x, y, (argb & 0xFF000000) | (Theme.INK.getRGB() & 0xFFFFFF));
                    }
            }
            logo = image;
            setPreferredSize(new Dimension(340, 130));
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (logo != null) {
                double scale = Math.min((getWidth() - 6.0) / logo.getWidth(), (getHeight() - 4.0) / logo.getHeight());
                int w = (int) (logo.getWidth() * scale), h = (int) (logo.getHeight() * scale);
                pen.drawImage(logo, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            } else {
                pen.setColor(Color.WHITE);
                pen.setFont(Theme.font(Font.BOLD, 30));
                pen.drawString("StackUp", 40, getHeight() / 2 + 10);
            }
            pen.dispose();
        }
    }
}

```

## src/frontend/PageSupport.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

/** Shared building blocks for every page; business rules remain in the backend. */
public abstract class PageSupport extends JPanel {
    private static final long serialVersionUID = 1L;
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    public static final int STYLE_WHITE = 0, STYLE_GREEN = 1, STYLE_DARK = 2;

    protected final DashboardUI window;
    protected final FinancialService service;
    protected final boolean admin;

    protected PageSupport(DashboardUI window) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        this.window = window;
        service = window.service();
        admin = service.getCurrent().getRole().equals("Admin");
    }

    /** Heading shown above the page; null uses the menu name. */
    protected String title() { return null; }
    protected String subtitle() { return ""; }
    /** Optional buttons shown at the right of the heading. */
    protected JComponent actions() { return null; }

    // ---------------------------------------------------------------- cards

    protected JPanel titled(String title, JComponent body) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 16));
        card.add(Theme.label(title, 18, Theme.INK), BorderLayout.NORTH);
        card.add(body);
        return card;
    }

    protected JPanel stat(String title, String value, String sub, int style, Icons.Kind icon) {
        return stat(title, value, sub, style, icon, style == STYLE_WHITE ? Theme.INK : Color.WHITE, 30);
    }

    /** A headline figure card in white, green or near-black. */
    protected JPanel stat(String title, String value, String sub, int style, Icons.Kind icon, Color valueColor, int size) {
        boolean solid = style != STYLE_WHITE;
        Theme.RoundedPanel card = style == STYLE_WHITE ? new Theme.RoundedPanel(Color.WHITE)
            : Theme.hero(style == STYLE_GREEN ? Theme.BRAND : Theme.INK);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.plain(title, 14, solid ? Color.WHITE : Theme.INK), BorderLayout.WEST);
        if (icon != null) head.add(Theme.badge(icon, solid ? new Color(255, 255, 255, 28) : Theme.SOFT,
            solid ? Color.WHITE : Theme.INK, 32), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        JPanel foot = Theme.clear(new BorderLayout(0, 6));
        foot.add(Theme.label(value, size, valueColor), BorderLayout.CENTER);
        if (sub != null && !sub.isEmpty()) foot.add(Theme.plain(sub, 13, solid ? new Color(222, 240, 231) : Theme.MUTED), BorderLayout.SOUTH);
        JPanel bottom = Theme.clear(new BorderLayout());
        bottom.add(foot, BorderLayout.SOUTH);
        card.add(bottom);
        return card;
    }

    /** Puts a small footer row (such as a change pill) under the big figure of a {@link #stat} card. */
    protected void addFoot(JPanel card, JComponent foot) {
        JPanel bottom = (JPanel) card.getComponent(1);
        JComponent figure = (JComponent) bottom.getComponent(0);
        JPanel stack = Theme.clear(new BorderLayout(0, 8));
        stack.add(figure, BorderLayout.CENTER);
        stack.add(foot, BorderLayout.SOUTH);
        bottom.removeAll();
        bottom.add(stack, BorderLayout.SOUTH);
    }

    /** Small rounded label such as "up 8%" with a direction arrow. */
    protected JComponent change(BigDecimal now, BigDecimal before, boolean onSolid, String suffix, boolean higherIsBetter) {
        JPanel row = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        Color muted = onSolid ? new Color(222, 240, 231) : Theme.MUTED;
        if (before.signum() == 0) {
            return row;
        }
        double percent = now.subtract(before).multiply(BigDecimal.valueOf(100))
            .divide(before.abs(), 0, RoundingMode.HALF_UP).doubleValue();
        boolean up = percent >= 0;
        boolean good = up == higherIsBetter;
        Color bg = onSolid ? Color.WHITE : good ? Theme.MINT : Theme.SOFT;
        Color fg = onSolid ? Theme.BRAND : good ? Theme.BRAND : Theme.INK;
        if (onSolid && !good) fg = Theme.INK;
        row.add(pill(String.format(Locale.US, "%.0f%%", Math.abs(percent)),
            Icons.of(up ? Icons.Kind.UP_RIGHT : Icons.Kind.DOWN_RIGHT, fg, 13), bg, fg));
        row.add(Theme.plain(suffix, 13, muted));
        return row;
    }

    protected static JComponent pill(String text, Icon icon, Color background, Color foreground) {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); setFont(Theme.font(Font.BOLD, 12)); }
            public Dimension getPreferredSize() {
                int width = getFontMetrics(getFont()).stringWidth(text) + 22 + (icon == null ? 0 : icon.getIconWidth() + 4);
                return new Dimension(width, 26);
            }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                pen.setColor(background);
                pen.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                int x = 10;
                if (icon != null) { icon.paintIcon(this, pen, x, (getHeight() - icon.getIconHeight()) / 2); x += icon.getIconWidth() + 4; }
                pen.setColor(foreground);
                pen.setFont(getFont());
                FontMetrics metrics = pen.getFontMetrics();
                pen.drawString(text, x, (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
                pen.dispose();
            }
        };
    }

    protected JPanel emptyState(String title, String subtitle) {
        JPanel panel = Theme.clear(new GridBagLayout());
        JPanel stack = Theme.clear(new GridLayout(3, 1, 0, 6));
        JPanel badge = Theme.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badge.add(Theme.badge(Icons.Kind.DOC, Theme.MINT, Theme.BRAND, 52));
        stack.add(badge);
        JLabel heading = Theme.label(title, 15, Theme.INK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(heading);
        JLabel detail = Theme.muted(subtitle, 13);
        detail.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(detail);
        panel.add(stack);
        return panel;
    }

    // --------------------------------------------------------------- tables

    /** Value for a two-line description cell. */
    public static final class Desc {
        final String title, sub;
        public Desc(String title, String sub) { this.title = title; this.sub = sub; }
        public String toString() { return title; }
    }

    /** Value for a description cell that starts with a small calendar tile. */
    public static final class Tile {
        final LocalDate date;
        final String text;
        public Tile(LocalDate date, String text) { this.date = date; this.text = text; }
        public String toString() { return text; }
    }

    protected DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    protected boolean isAdminUser(String name) {
        if (name == null) return false;
        if (name.equalsIgnoreCase(service.getCurrent().getUsername())) return admin;
        if (!admin) return false;
        for (Account account : service.getAccounts())
            if (account.getUsername().equalsIgnoreCase(name)) return account.getRole().equals("Admin");
        return false;
    }

    /**
     * Styled table. Kinds per column: text, bold, muted, money, chip, user, desc, tile.
     */
    protected JTable table(DefaultTableModel model, String... kinds) {
        JTable table = new JTable(model);
        table.setRowHeight(46);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setOpaque(true);
        table.setBackground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionBackground(Theme.SELECTED);
        table.setSelectionForeground(Theme.INK);
        table.setRowSelectionAllowed(true);
        table.setFont(Theme.font(Font.PLAIN, 14));
        CellRenderer renderer = new CellRenderer(kinds);
        for (int column = 0; column < model.getColumnCount(); column++)
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(300, 40));
        header.setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel label = new JLabel(String.valueOf(value));
            label.setOpaque(true);
            label.setBackground(Theme.BACKGROUND);
            label.setForeground(Theme.MUTED);
            label.setFont(Theme.font(Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(0, 18, 0, 8));
            return label;
        });
        return table;
    }

    protected void widths(JTable table, int... weights) {
        for (int i = 0; i < weights.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(weights[i]);
    }

    private final class CellRenderer implements TableCellRenderer {
        private final String[] kinds;
        CellRenderer(String[] kinds) { this.kinds = kinds; }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            String kind = column < kinds.length ? kinds[column] : "text";
            String text = value == null ? "" : value.toString();
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(selected ? Theme.SELECTED : Color.WHITE);
            boolean last = row == table.getRowCount() - 1;
            cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, last ? 0 : 1, 0, Theme.LINE),
                new EmptyBorder(0, 18, 0, 8)));
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            switch (kind) {
                case "check": {
                    boolean on = selected;
                    c.weightx = 0;
                    cell.add(new JComponent() {
                        private static final long serialVersionUID = 1L;
                        { setOpaque(false); }
                        public Dimension getPreferredSize() { return new Dimension(20, 20); }
                        protected void paintComponent(Graphics graphics) {
                            Graphics2D pen = (Graphics2D) graphics.create();
                            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            pen.setColor(on ? Theme.BRAND : Color.WHITE);
                            pen.fill(new RoundRectangle2D.Double(1, 1, 17, 17, 6, 6));
                            pen.setColor(on ? Theme.BRAND : new Color(120, 130, 125));
                            pen.draw(new RoundRectangle2D.Double(1, 1, 17, 17, 6, 6));
                            if (on) Icons.of(Icons.Kind.CHECK, Color.WHITE, 14).paintIcon(this, pen, 3, 3);
                            pen.dispose();
                        }
                    }, c);
                    break;
                }
                case "chip":
                    c.weightx = 0;
                    cell.add(new Theme.Chip(text), c);
                    c.weightx = 1; cell.add(Box.createGlue(), c);
                    break;
                case "user": {
                    c.weightx = 0;
                    cell.add(new Theme.Avatar(text, isAdminUser(text), 32), c);
                    c.insets = new Insets(0, 10, 0, 0); c.weightx = 1;
                    cell.add(Theme.plain(text, 14, Theme.INK), c);
                    break;
                }
                case "desc": {
                    Desc desc = (Desc) value;
                    JPanel two = Theme.clear(new GridLayout(2, 1, 0, 1));
                    two.add(Theme.plain(desc.title, 14, Theme.INK));
                    two.add(Theme.muted(desc.sub, 13));
                    cell.add(two, c);
                    break;
                }
                case "tile": {
                    Tile tile = (Tile) value;
                    c.weightx = 0;
                    cell.add(dateTile(tile.date), c);
                    c.insets = new Insets(0, 12, 0, 0); c.weightx = 1;
                    cell.add(Theme.plain(tile.text, 14, Theme.INK), c);
                    break;
                }
                case "money": {
                    JLabel label = Theme.label(text, 14, text.startsWith("+") ? Theme.BRAND : Theme.INK);
                    cell.add(label, c);
                    break;
                }
                case "bold":
                    cell.add(Theme.label(text, 14, Theme.INK), c);
                    break;
                case "muted":
                    cell.add(Theme.plain(text, 13, Theme.MUTED), c);
                    break;
                default:
                    cell.add(Theme.plain(text, 14, Theme.INK), c);
            }
            return cell;
        }
    }

    private JComponent dateTile(LocalDate date) {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); }
            public Dimension getPreferredSize() { return new Dimension(44, 44); }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                pen.setColor(Theme.SOFT);
                pen.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                String month = date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
                pen.setColor(Theme.BRAND);
                pen.setFont(Theme.font(Font.BOLD, 10));
                pen.drawString(month, (getWidth() - pen.getFontMetrics().stringWidth(month)) / 2, 16);
                pen.setColor(Theme.INK);
                pen.setFont(Theme.font(Font.BOLD, 16));
                String day = Integer.toString(date.getDayOfMonth());
                pen.drawString(day, (getWidth() - pen.getFontMetrics().stringWidth(day)) / 2, 34);
                pen.dispose();
            }
        };
    }

    /** Rounded frame holding the table header and rows; shows a friendly message when empty. */
    protected JPanel tableArea(JTable table, String message) {
        Theme.RoundedPanel box = new Theme.RoundedPanel(Color.WHITE);
        box.radius = 22;
        box.outlined = true;
        box.clip = true;
        box.setLayout(new BorderLayout());
        box.add(table.getTableHeader(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(table, BorderLayout.NORTH);
        JPanel empty = emptyState(message, "Entries will show up here as soon as they exist.");
        empty.setPreferredSize(new Dimension(100, 130));
        empty.setVisible(table.getRowCount() == 0);
        table.getModel().addTableModelListener(event -> empty.setVisible(table.getRowCount() == 0));
        body.add(empty);
        box.add(body);
        return box;
    }

    // ------------------------------------------------------------- helpers

    protected static String signed(FinancialRecord record) {
        return (record.signedAmount().signum() > 0 ? "+" : "−") + Theme.money(record.getAmount());
    }

    protected void error(Exception exception) {
        Theme.message(this, "Check input", exception.getMessage());
    }

    protected void record(String type, FinancialRecord existing) {
        if (Dialogs.record(this, service, type, existing)) window.refresh();
    }

    protected void schedule() {
        if (Dialogs.payment(this, service)) window.refresh();
    }

    /** Status words for a payment: Paid, Due today, Due in 3 days, Scheduled... */
    protected static String dueText(Payment payment) {
        if (payment.isPaid()) return "Paid";
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), payment.getDue());
        if (days < 0) return "Due " + (-days) + " days ago";
        if (days == 0) return "Due today";
        if (days <= 7) return "Due in " + days + " day" + (days == 1 ? "" : "s");
        return "Scheduled";
    }

    protected static LocalDate monthStart() { return LocalDate.now().withDayOfMonth(1); }
    protected static LocalDate monthEnd() { return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()); }

    protected BigDecimal totalIn(String type, LocalDate from, LocalDate to) { return service.total(type, from, to); }


}

```

## src/frontend/Pager.java

```java
package frontend;

import frontend.style.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** "Showing 1-7 of 128" with numbered page buttons. */
public final class Pager extends Theme.SmoothPanel {
    private static final long serialVersionUID = 1L;
    private final int size;
    private final Runnable onChange;
    private int page, total;

    public Pager(int size, Runnable onChange) {
        super(new BorderLayout());
        this.size = size;
        this.onChange = onChange;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
    }

    public int from() { return page * size; }
    public int to() { return Math.min(total, from() + size); }
    public void reset() { page = 0; }

    public void update(int total) {
        this.total = total;
        int pages = Math.max(1, (total + size - 1) / size);
        page = Math.min(page, pages - 1);
        removeAll();
        setVisible(pages > 1);
        JPanel buttons = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(square("‹", page > 0, false, page - 1));
        for (int number : numbers(pages)) {
            if (number < 0) buttons.add(square("…", false, false, 0));
            else buttons.add(square(Integer.toString(number + 1), true, number == page, number));
        }
        buttons.add(square("›", page < pages - 1, false, page + 1));
        add(buttons, BorderLayout.EAST);
        revalidate();
        repaint();
    }

    /** Page indexes to show; -1 stands for an ellipsis. */
    private List<Integer> numbers(int pages) {
        List<Integer> list = new ArrayList<>();
        if (pages <= 6) {
            for (int i = 0; i < pages; i++) list.add(i);
            return list;
        }
        list.add(0);
        if (page > 2) list.add(-1);
        for (int i = Math.max(1, page - 1); i <= Math.min(pages - 2, page + 1); i++) list.add(i);
        if (page < pages - 3) list.add(-1);
        list.add(pages - 1);
        return list;
    }

    private JButton square(String text, boolean enabled, boolean active, int target) {
        JButton button = active ? Theme.dark(text) : Theme.outline(text);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(32, 32));
        button.setEnabled(enabled);
        button.addActionListener(event -> { page = target; onChange.run(); });
        return button;
    }
}

```

## src/frontend/PaymentsUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Schedule bills ahead of time and tick them off as they are paid. */
public final class PaymentsUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private boolean showPaid;

    /** Each click ticks or unticks a row, like a checkbox list. */
    private static final class TickSelection extends DefaultListSelectionModel {
        private static final long serialVersionUID = 1L;
        @Override public void setSelectionInterval(int from, int to) {
            if (from == to && from >= 0) {
                if (isSelectedIndex(from)) removeSelectionInterval(from, from);
                else addSelectionInterval(from, from);
            } else super.setSelectionInterval(from, to);
        }
    }

    public PaymentsUI(DashboardUI window) {
        super(window);
        List<Payment> all = service.getPayments();
        LocalDate today = LocalDate.now();
        BigDecimal upcomingTotal = BigDecimal.ZERO, weekTotal = BigDecimal.ZERO;
        int upcomingCount = 0;
        Payment nextDue = null;
        for (Payment payment : all) {
            if (payment.isPaid()) continue;
            upcomingTotal = upcomingTotal.add(payment.getAmount());
            upcomingCount++;
            if (ChronoUnit.DAYS.between(today, payment.getDue()) <= 7) weekTotal = weekTotal.add(payment.getAmount());
            if (nextDue == null || payment.getDue().isBefore(nextDue.getDue())) nextDue = payment;
        }
        JPanel stats = Theme.equal(12,
            stat("Upcoming Total", Theme.whole(upcomingTotal), upcomingCount + " scheduled payment" + (upcomingCount == 1 ? "" : "s"), STYLE_GREEN, Icons.Kind.CLOCK, Color.WHITE, 26),
            stat("Due This Week", Theme.whole(weekTotal), nextDue == null ? "Nothing scheduled" : nextDue.getDescription() + " · " + nextDue.getDue().format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)),
                STYLE_WHITE, Icons.Kind.CALENDAR, Theme.INK, 26));
        stats.setPreferredSize(new Dimension(700, 120));

        DefaultTableModel rows = model("", "Description", "Category", "Amount", "Due Date", "Scheduled By", "Status");
        JTable table = table(rows, "check", "tile", "muted", "bold", "muted", "user", "chip");
        table.setSelectionModel(new TickSelection());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowHeight(52);
        widths(table, 36, 210, 100, 110, 110, 120, 130);
        JTextField search = Theme.search("Search payments");
        search.setPreferredSize(new Dimension(300, 38));
        JLabel selection = Theme.muted("", 14);
        JButton pay = Theme.button("Mark Selected Paid");
        pay.setIcon(Icons.of(Icons.Kind.CHECK, Color.WHITE, 16));
        pay.setEnabled(false);
        List<Payment> visible = new ArrayList<>();
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            visible.clear();
            rows.setRowCount(0);
            String needle = search.getText().trim().toLowerCase(Locale.ROOT);
            List<Payment> sorted = new ArrayList<>(service.getPayments());
            sorted.sort(showPaid ? Comparator.comparing(Payment::getDue).reversed() : Comparator.comparing(Payment::getDue));
            for (Payment payment : sorted) {
                if (payment.isPaid() != showPaid) continue;
                if (!(payment.getDescription() + " " + payment.getCategory()).toLowerCase(Locale.ROOT).contains(needle)) continue;
                visible.add(payment);
                String by = payment.getScheduledBy() == null ? "—" : payment.getScheduledBy();
                rows.addRow(new Object[]{"", new Tile(payment.getDue(), payment.getDescription()), payment.getCategory(),
                    Theme.money(payment.getAmount()), payment.getDue().format(DAY), by, dueText(payment)});
            }
            pay.setVisible(!showPaid);
            selection.setText(showPaid ? "" : "0 selected");
        };
        JPanel tabs = Theme.segmented(new String[]{"Upcoming Payments", "Paid Payments"}, 0, Theme.INK, index -> {
            showPaid = index == 1;
            refresh[0].run();
        });
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent event) { refresh[0].run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent event) { refresh[0].run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent event) { }
        });
        table.getSelectionModel().addListSelectionListener(event -> {
            int[] chosen = table.getSelectedRows();
            BigDecimal sum = BigDecimal.ZERO;
            for (int row : chosen) if (row < visible.size()) sum = sum.add(visible.get(row).getAmount());
            pay.setEnabled(chosen.length > 0 && !showPaid);
            selection.setText(showPaid ? "" : chosen.length + " selected" + (chosen.length > 0 ? " · " + Theme.money(sum) : ""));
        });
        pay.addActionListener(event -> {
            int[] chosen = table.getSelectedRows();
            if (chosen.length == 0) return;
            String question = chosen.length == 1 ? "Record this payment as an expense?" : "Record these " + chosen.length + " payments as expenses?";
            if (!Theme.confirm(this, Icons.Kind.CHECK, "Mark as paid?", question, "Yes, mark paid", "Cancel")) return;
            try {
                for (int row : chosen) service.pay(visible.get(row).getId());
                window.refresh();
            } catch (Exception exception) { error(exception); }
        });

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        head.add(tabs, BorderLayout.WEST);
        head.add(search, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        card.add(tableArea(table, "No payments in this list yet."));
        JPanel foot = Theme.clear(new BorderLayout());
        foot.add(selection, BorderLayout.WEST);
        foot.add(pay, BorderLayout.EAST);
        card.add(foot, BorderLayout.SOUTH);

        JPanel content = Theme.clear(new BorderLayout(0, 12));
        content.add(stats, BorderLayout.NORTH);
        content.add(card);
        add(content);
        refresh[0].run();
    }


    @Override protected String subtitle() { return "Schedule bills ahead of time and mark them paid when they go out."; }

    @Override protected JComponent actions() {
        if (admin) return null;
        JButton schedule = Theme.dark("Schedule Payment");
        schedule.setIcon(Icons.of(Icons.Kind.PLUS, Color.WHITE, 18));
        schedule.addActionListener(event -> schedule());
        return schedule;
    }
}

```

## src/frontend/TransactionHistoryUI.java

```java
package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Every revenue and expense with filters; admins can also edit or delete a record. */
public final class TransactionHistoryUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private final List<FinancialRecord> matches = new ArrayList<>();
    private final List<FinancialRecord> visible = new ArrayList<>();
    private String type = "All";

    public TransactionHistoryUI(DashboardUI window) {
        super(window);
        JPanel top = Theme.clear(new BorderLayout(0, 12));

        // ---- filters
        JTextField search = Theme.search("Search description, category or user");
        DatePicker from = new DatePicker(), to = new DatePicker();
        JButton apply = Theme.button("Apply"), reset = Theme.outline("Reset");
        Runnable[] refresh = new Runnable[1];
        JPanel tabs = Theme.segmented(new String[]{"All", "Revenue", "Expenses"}, 0, Theme.INK, index -> {
            type = index == 0 ? "All" : index == 1 ? "Revenue" : "Expense";
            refresh[0].run();
        });
        JPanel filters = Theme.card();
        filters.setLayout(new BorderLayout(0, 14));
        JPanel line = Theme.clear(new BorderLayout(14, 0));
        line.add(tabs, BorderLayout.WEST);
        line.add(search);
        filters.add(line, BorderLayout.NORTH);
        JPanel dates = Theme.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.insets = new Insets(0, 0, 0, 14);
        c.gridx = 0; dates.add(Theme.field("From date", from), c);
        c.gridx = 1; dates.add(Theme.field("To date", to), c);
        c.gridx = 2; c.weightx = 0; c.anchor = GridBagConstraints.SOUTH; c.fill = GridBagConstraints.NONE;
        apply.setPreferredSize(new Dimension(130, 44));
        dates.add(apply, c);
        c.gridx = 3; c.insets = new Insets(0, 0, 0, 0);
        reset.setPreferredSize(new Dimension(130, 44));
        dates.add(reset, c);
        filters.add(dates);
        top.add(filters, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- table
        DefaultTableModel rows = model("Description", "Category", "Type", "Amount", "Date", "Recorded By");
        JTable table = table(rows, "text", "muted", "chip", "money", "muted", "user");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        widths(table, 200, 110, 100, 130, 110, 130);
        JLabel selection = Theme.muted("", 14);
        Pager pager = new Pager(5, () -> refresh[0].run());
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 16));
        JPanel head = Theme.clear(new BorderLayout());
        JPanel title = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.add(Theme.label("All Transactions", 22, Theme.INK));
        title.add(selection);
        head.add(title, BorderLayout.WEST);
        JButton edit = null, delete = null;
        if (admin) {
            edit = Theme.outline("Edit Selected");
            delete = Theme.outline("Delete Selected");
            edit.setEnabled(false);
            delete.setEnabled(false);
            JPanel actions = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            actions.add(edit);
            actions.add(delete);
            head.add(actions, BorderLayout.EAST);
        }
        card.add(head, BorderLayout.NORTH);
        card.add(tableArea(table, "No transactions found."));
        card.add(pager, BorderLayout.SOUTH);
        add(card);

        refresh[0] = () -> {
            try {
                LocalDate start = from.getText().isBlank() ? LocalDate.MIN : from.getSelectedDate();
                LocalDate end = to.getText().isBlank() ? LocalDate.MAX : to.getSelectedDate();
                if (start.isAfter(end)) throw new IllegalArgumentException("From date must be before To date.");
                String needle = search.getText().trim().toLowerCase(Locale.ROOT);
                matches.clear();
                List<FinancialRecord> all = new ArrayList<>(service.getRecords());
                Collections.reverse(all);
                all.sort((a, b) -> b.getDate().compareTo(a.getDate()));
                for (FinancialRecord record : all) {
                    LocalDate day = record.getDate().toLocalDate();
                    if ((type.equals("All") || record.getType().equals(type))
                        && (record.getDescription() + " " + record.getCategory() + " " + record.getRecordedBy()).toLowerCase(Locale.ROOT).contains(needle)
                        && !day.isBefore(start) && !day.isAfter(end)) {
                        matches.add(record);
                    }
                }
                pager.update(matches.size());
                visible.clear();
                rows.setRowCount(0);
                for (FinancialRecord record : matches.subList(pager.from(), pager.to())) {
                    visible.add(record);
                    rows.addRow(new Object[]{record.getDescription(), record.getCategory(), record.getType(), signed(record),
                        record.getDate().toLocalDate().format(DAY), record.getRecordedBy()});
                }
                selection.setText("");
            } catch (Exception exception) { error(exception); }
        };
        apply.addActionListener(event -> { pager.reset(); refresh[0].run(); });
        search.addActionListener(event -> { pager.reset(); refresh[0].run(); });
        reset.addActionListener(event -> {
            search.setText(""); from.setSelectedDate(null); to.setSelectedDate(null);
            pager.reset(); refresh[0].run();
        });
        if (admin) {
            JButton editButton = edit, deleteButton = delete;
            table.getSelectionModel().addListSelectionListener(event -> {
                boolean chosen = table.getSelectedRow() >= 0;
                editButton.setEnabled(chosen);
                deleteButton.setEnabled(chosen);
                selection.setText(chosen ? "· 1 selected" : "");
            });
            editButton.addActionListener(event -> {
                int row = table.getSelectedRow();
                if (row >= 0) record(visible.get(row).getType(), visible.get(row));
            });
            deleteButton.addActionListener(event -> {
                int row = table.getSelectedRow();
                if (row >= 0 && Theme.confirm(this, Icons.Kind.CLOSE, "Delete transaction?", "This removes the record for good and is saved in the activity log.", "Yes, delete", "Cancel")) {
                    try { service.deleteRecord(visible.get(row).getId()); refresh[0].run(); }
                    catch (Exception exception) { error(exception); }
                }
            });
        }
        refresh[0].run();
    }

    @Override protected String subtitle() { return "Every revenue and expense recorded, with who recorded it."; }

}

```

## src/frontend/style/ButtonSkin.java

```java
package frontend.style;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/** Fully rounded buttons: solid, outlined or ghost, with a gentle hover and press state. */
public final class ButtonSkin extends BasicButtonUI {
    public static ComponentUI createUI(JComponent component) { return new ButtonSkin(); }

    protected void installDefaults(AbstractButton button) {
        super.installDefaults(button);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        if (button.getBorder() instanceof javax.swing.plaf.UIResource)
            button.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
    }

    public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean ghost = Boolean.TRUE.equals(button.getClientProperty("ghost"));
        boolean outline = Boolean.TRUE.equals(button.getClientProperty("outline"));
        ButtonModel model = button.getModel();
        Color base = button.getBackground();
        if (!button.isEnabled()) base = Theme.SOFT;
        else if (model.isPressed()) base = ghost ? Theme.MINT : shade(base, 0.82f);
        else if (model.isRollover()) base = ghost ? new Color(0, 0, 0, 14) : outline ? Theme.BACKGROUND : shade(base, 0.9f);
        int arc = button.getHeight();
        if (!(ghost && !model.isRollover() && !model.isPressed())) {
            pen.setColor(base);
            pen.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), arc, arc);
        }
        if (outline && button.isEnabled()) {
            pen.setColor(Theme.LINE);
            pen.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, arc, arc);
        }
        pen.dispose();
        super.paint(graphics, component);
    }

    private static Color shade(Color color, float factor) {
        return new Color(Math.round(color.getRed() * factor), Math.round(color.getGreen() * factor),
            Math.round(color.getBlue() * factor), color.getAlpha());
    }

    protected void paintText(Graphics graphics, JComponent component, Rectangle textRect, String text) {
        AbstractButton button = (AbstractButton) component;
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.setColor(button.isEnabled() ? button.getForeground() : new Color(150, 158, 154));
        graphics.setFont(Theme.fontFor(text, button.getFont().getStyle(), button.getFont().getSize2D()));
        metrics = graphics.getFontMetrics();
        graphics.drawString(text, textRect.x, textRect.y + metrics.getAscent());
    }
}

```

## src/frontend/style/ComboBoxSkin.java

```java
package frontend.style;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Rounded white drop-down matching the text inputs. */
public final class ComboBoxSkin extends BasicComboBoxUI {
    public static ComponentUI createUI(JComponent component) { return new ComboBoxSkin(); }

    protected void installDefaults() {
        super.installDefaults();
        comboBox.setOpaque(false);
        comboBox.setFont(Theme.font(Font.PLAIN, 14));
    }

    protected JButton createArrowButton() {
        JButton arrow = new JButton(Icons.of(Icons.Kind.CHEVRON, Theme.INK, 16));
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));
        arrow.setContentAreaFilled(false);
        arrow.putClientProperty("ghost", Boolean.TRUE);
        arrow.setBackground(new Color(0, 0, 0, 0));
        arrow.setFocusPainted(false);
        arrow.setOpaque(false);
        return arrow;
    }

    public void paint(Graphics graphics, JComponent component) {
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = component.getWidth(), h = component.getHeight();
        pen.setColor(component.isEnabled() ? Color.WHITE : Theme.SOFT);
        pen.fill(new RoundRectangle2D.Double(0, 0, w, h, 28, 28));
        pen.setColor(comboBox.hasFocus() ? Theme.BRAND : Theme.LINE);
        pen.draw(new RoundRectangle2D.Double(.5, .5, w - 1, h - 1, 28, 28));
        pen.dispose();
        super.paint(graphics, component);
    }

    public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) { }

    protected ListCellRenderer<Object> createRenderer() {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setFont(Theme.font(Font.PLAIN, 14));
                label.setBorder(BorderFactory.createEmptyBorder(6, index < 0 ? 2 : 12, 6, 12));
                label.setOpaque(index >= 0);
                label.setBackground(selected ? Theme.MINT : Color.WHITE);
                label.setForeground(Theme.INK);
                return label;
            }
        };
    }
}

```

## src/frontend/style/Icons.java

```java
package frontend.style;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Thin line icons drawn on a 24-unit grid, so they stay crisp at any size and colour. */
public final class Icons {
    public enum Kind {
        HOME, PLUS_CIRCLE, CLOCK, DOC, BARS, PIE, PULSE, USERS, HELP, LOGOUT, SEARCH, BELL, INFO,
        CHEVRON, DOWNLOAD, CALENDAR, TREND, WALLET, CHECK, UP_RIGHT, DOWN_RIGHT, PLUS, MINUS, WIFI, WINDOW_CHECK, CLOSE
    }

    private Icons() {}

    public static Icon of(Kind kind, Color color, int size) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                pen.translate(x, y);
                pen.scale(size / 24.0, size / 24.0);
                pen.setColor(color);
                pen.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(pen, kind);
                pen.dispose();
            }
        };
    }

    private static void line(Graphics2D pen, double x1, double y1, double x2, double y2) {
        pen.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D pen, double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        pen.draw(path);
    }

    private static void circle(Graphics2D pen, double cx, double cy, double r) {
        pen.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void draw(Graphics2D pen, Kind kind) {
        switch (kind) {
            case HOME:
                poly(pen, 3.5, 11, 12, 3.8, 20.5, 11);
                poly(pen, 6, 9.5, 6, 20, 18, 20, 18, 9.5);
                break;
            case PLUS_CIRCLE:
                circle(pen, 12, 12, 9); line(pen, 12, 7.5, 12, 16.5); line(pen, 7.5, 12, 16.5, 12);
                break;
            case CLOCK:
                circle(pen, 12, 12, 9); poly(pen, 12, 6.8, 12, 12, 15.6, 14);
                break;
            case DOC:
                pen.draw(new RoundRectangle2D.Double(5, 3, 14, 18, 4, 4));
                line(pen, 8.6, 9, 15.4, 9); line(pen, 8.6, 13, 15.4, 13); line(pen, 8.6, 17, 12.4, 17);
                break;
            case BARS:
                line(pen, 6.5, 20, 6.5, 14); line(pen, 12, 20, 12, 5); line(pen, 17.5, 20, 17.5, 10);
                break;
            case PIE:
                pen.draw(new Arc2D.Double(3, 5, 16, 16, 90, 270, Arc2D.PIE));
                pen.draw(new Arc2D.Double(12, 2, 9.5, 9.5, 0, 90, Arc2D.PIE));
                break;
            case PULSE:
                poly(pen, 2.5, 12, 7, 12, 10, 4, 14, 20, 17, 12, 21.5, 12);
                break;
            case USERS:
                circle(pen, 9, 8, 3.6);
                pen.draw(new Arc2D.Double(2.8, 14, 12.4, 12, 0, 180, Arc2D.OPEN));
                pen.draw(new Arc2D.Double(13.2, 4.6, 6.4, 6.4, -80, 200, Arc2D.OPEN));
                pen.draw(new Arc2D.Double(15, 13.2, 6.6, 9, 0, 150, Arc2D.OPEN));
                break;
            case HELP:
                circle(pen, 12, 12, 9);
                pen.draw(new Arc2D.Double(9, 7.3, 6, 5.4, 160, -230, Arc2D.OPEN));
                poly(pen, 12, 12.4, 12, 14.2);
                pen.fill(new Ellipse2D.Double(11.1, 16, 1.8, 1.8));
                break;
            case LOGOUT:
                poly(pen, 10, 7, 5, 12, 10, 17); line(pen, 5, 12, 15, 12);
                poly(pen, 14, 4.5, 20, 4.5, 20, 19.5, 14, 19.5);
                break;
            case SEARCH:
                circle(pen, 10.8, 10.8, 6.6); line(pen, 15.8, 15.8, 21, 21);
                break;
            case BELL:
                pen.draw(new Arc2D.Double(6, 3.5, 12, 12, 0, 180, Arc2D.OPEN));
                line(pen, 6, 9.5, 6, 16); line(pen, 18, 9.5, 18, 16); line(pen, 3.8, 16.5, 20.2, 16.5);
                pen.draw(new Arc2D.Double(9.4, 17.4, 5.2, 4.4, 180, 180, Arc2D.OPEN));
                break;
            case INFO:
                circle(pen, 12, 12, 9); line(pen, 12, 11, 12, 17);
                pen.fill(new Ellipse2D.Double(11, 6.4, 2, 2));
                break;
            case CHEVRON:
                poly(pen, 6, 9.2, 12, 15, 18, 9.2);
                break;
            case DOWNLOAD:
                line(pen, 12, 4, 12, 15); poly(pen, 7, 10.4, 12, 15.4, 17, 10.4); line(pen, 5, 20, 19, 20);
                break;
            case CALENDAR:
                pen.draw(new RoundRectangle2D.Double(3.8, 5, 16.4, 15.4, 4, 4));
                line(pen, 3.8, 10, 20.2, 10); line(pen, 8, 3, 8, 7); line(pen, 16, 3, 16, 7);
                break;
            case TREND:
                poly(pen, 4, 15.5, 8.5, 11, 12.5, 14.5, 20, 7.5);
                break;
            case WALLET:
                pen.draw(new RoundRectangle2D.Double(3, 5.5, 18, 13, 4, 4));
                line(pen, 3, 10, 21, 10); line(pen, 16, 14.5, 18, 14.5);
                break;
            case CHECK:
                poly(pen, 5, 12.6, 10, 17.2, 19, 7);
                break;
            case UP_RIGHT:
                line(pen, 7, 17, 17, 7); poly(pen, 8, 7, 17, 7, 17, 16);
                break;
            case DOWN_RIGHT:
                line(pen, 7, 7, 17, 17); poly(pen, 17, 8, 17, 17, 8, 17);
                break;
            case PLUS:
                line(pen, 12, 5, 12, 19); line(pen, 5, 12, 19, 12);
                break;
            case MINUS:
                line(pen, 5, 12, 19, 12);
                break;
            case WINDOW_CHECK:
                pen.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
                line(pen, 3, 9, 21, 9);
                poly(pen, 8.5, 14.5, 11, 17, 15.5, 12.2);
                break;
            case CLOSE:
                line(pen, 6, 6, 18, 18); line(pen, 18, 6, 6, 18);
                break;
            case WIFI:
                pen.draw(new Arc2D.Double(5, 7, 14, 14, 45, 90, Arc2D.OPEN));
                pen.draw(new Arc2D.Double(8.4, 11.4, 7.2, 7.2, 45, 90, Arc2D.OPEN));
                pen.fill(new Ellipse2D.Double(10.9, 16.6, 2.4, 2.4));
                pen.draw(new Arc2D.Double(1.8, 3.2, 20.4, 20.4, 45, 90, Arc2D.OPEN));
                break;
            default:
                break;
        }
    }
}

```

## src/frontend/style/ScrollBarSkin.java

```java
package frontend.style;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Slim, buttonless scroll bars that stay out of the way of the cards. */
public final class ScrollBarSkin extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent component) { return new ScrollBarSkin(); }

    protected JButton createDecreaseButton(int orientation) { return empty(); }
    protected JButton createIncreaseButton(int orientation) { return empty(); }
    private JButton empty() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        return button;
    }
    public Dimension getPreferredSize(JComponent component) { return new Dimension(10, 10); }

    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) { }

    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setColor(isThumbRollover() || isDragging ? new Color(150, 160, 155) : new Color(188, 196, 192));
        boolean vertical = scrollbar.getOrientation() == Adjustable.VERTICAL;
        int x = vertical ? bounds.x + 2 : bounds.x, y = vertical ? bounds.y : bounds.y + 2;
        int w = vertical ? bounds.width - 4 : bounds.width, h = vertical ? bounds.height : bounds.height - 4;
        pen.fillRoundRect(x, y, w, h, 6, 6);
        pen.dispose();
    }
}

```

## src/frontend/style/Theme.java

```java
package frontend.style;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Design system: green and near-black on soft grey, pill controls and large rounded cards. */
public final class Theme {
    public static final Color BRAND = new Color(11, 128, 82);
    public static final Color BRAND_LIGHT = new Color(23, 148, 98);
    public static final Color BRAND_MID = new Color(66, 196, 143);
    public static final Color BRAND_PALE = new Color(168, 222, 193);
    public static final Color MINT = new Color(225, 245, 235);
    public static final Color INK = new Color(17, 22, 20);
    public static final Color BACKGROUND = new Color(243, 245, 244);
    public static final Color FRAME = new Color(229, 233, 232);
    public static final Color LINE = new Color(225, 230, 228);
    public static final Color SOFT = new Color(238, 241, 240);
    public static final Color MUTED = new Color(100, 111, 106);
    public static final Color SELECTED = new Color(236, 246, 240);
    public static final Color DARK_CARD_CIRCLE = new Color(255, 255, 255, 14);
    public static final Color BRAND_CARD_CIRCLE = new Color(255, 255, 255, 22);
    /** Retained names so older call sites read naturally. */
    public static final Color TEAL = BRAND, BLACK = INK, GREEN = BRAND;
    public static final Color ERROR = new Color(190, 52, 52);

    private Theme() {}

    public static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        for (String name : new String[]{"Inter-Regular.ttf", "Inter-Bold.ttf"}) {
            try { GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
                Font.createFont(Font.TRUETYPE_FONT, asset(name))); }
            catch (Exception ignored) { }
        }
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }
        Font font = font(Font.PLAIN, 13);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "Table.font", "TableHeader.font", "ComboBox.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ToolTip.font", "MenuItem.font", "PopupMenu.font"})
            UIManager.put(key, font);
        UIManager.put("ButtonUI", "frontend.style.ButtonSkin");
        UIManager.put("ComboBoxUI", "frontend.style.ComboBoxSkin");
        UIManager.put("ScrollBarUI", "frontend.style.ScrollBarSkin");
        UIManager.put("ComboBox.border", new InputBorder());
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", SOFT);
        UIManager.put("Button.foreground", INK);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", INK);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("OptionPane.border", new EmptyBorder(18, 20, 14, 20));
        UIManager.put("OptionPane.buttonAreaBorder", new EmptyBorder(8, 0, 0, 0));
        UIManager.put("PopupMenu.background", Color.WHITE);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(LINE));
        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", INK);
        UIManager.put("MenuItem.selectionBackground", MINT);
        UIManager.put("MenuItem.selectionForeground", INK);
        UIManager.put("ToolTip.background", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", new EmptyBorder(4, 8, 4, 8));
    }

    /**
     * Finds a file in the assets folder wherever the app was started from: the working
     * directory, a StackUpMockupEdition folder inside it, or any folder above the compiled classes.
     */
    public static java.io.File asset(String name) {
        java.util.List<java.io.File> roots = new java.util.ArrayList<>();
        roots.add(new java.io.File("."));
        roots.add(new java.io.File("StackUpMockupEdition"));
        try {
            java.io.File here = new java.io.File(Theme.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (java.io.File dir = here; dir != null; dir = dir.getParentFile()) roots.add(dir);
        } catch (Exception ignored) { }
        for (java.io.File root : roots) {
            java.io.File file = new java.io.File(new java.io.File(root, "assets"), name);
            if (file.isFile()) return file;
        }
        return new java.io.File("assets", name);
    }

    public static Font font(int style, float size) {
        return new Font("Inter", style, 12).deriveFont(size);
    }

    /** Inter covers the peso sign, so every string uses the same family. */
    public static Font fontFor(String text, int style, float size) {
        return font(style, size);
    }

    public static String money(BigDecimal value) {
        return "₱" + String.format(Locale.US, "%,.2f", value);
    }

    public static String whole(BigDecimal value) {
        return "₱" + String.format(Locale.US, "%,.0f", value);
    }

    public static String compact(double value) {
        double abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1_000_000) return sign + "₱" + trim(abs / 1_000_000) + "M";
        if (abs >= 1_000) return sign + "₱" + trim(abs / 1_000) + "k";
        return sign + "₱" + trim(abs);
    }

    private static String trim(double value) {
        String text = String.format(Locale.US, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(fontFor(text, Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    public static JLabel muted(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(fontFor(text, Font.PLAIN, size));
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel plain(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(fontFor(text, Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    /** "Page already open" pop-up: one dark button, no choice to make. */
    public static void alreadyOpen(Component owner, String page) {
        popup(owner, Icons.Kind.WINDOW_CHECK, "Page already open",
            "You're already on <b>" + page + "</b>. Pick another section from the sidebar to switch pages.",
            "OK, got it", null);
    }

    /** Themed replacement for JOptionPane.showMessageDialog: title, plain-text message, one OK button. */
    public static void message(Component owner, String title, String message) {
        String safe = String.valueOf(message).replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        popup(owner, Icons.Kind.INFO, title, safe, "OK", null);
    }

    /** Themed yes/no question. Returns true when the main button was pressed. */
    public static boolean confirm(Component owner, Icons.Kind icon, String title, String message, String yes, String no) {
        return popup(owner, icon, title, message, yes, no);
    }

    /**
     * Rounded white pop-up used for every message and question: an icon badge, a close button,
     * a title, a short message and one or two pill buttons. A hidden JOptionPane holds the
     * answer so the window closes as soon as a button sets its value.
     */
    private static boolean popup(Component owner, Icons.Kind icon, String title, String message, String yes, String no) {
        Window parent = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JOptionPane result = new JOptionPane("", JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[0]);
        result.setVisible(false);
        result.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, event -> dialog.dispose());

        RoundedPanel card = new RoundedPanel(Color.WHITE);
        card.radius = 30;
        card.outlined = true;
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel top = clear(new BorderLayout());
        top.add(badge(icon, MINT, BRAND, 56), BorderLayout.WEST);
        JButton close = new JButton(Icons.of(Icons.Kind.CLOSE, INK, 16));
        close.setBackground(SOFT);
        close.setPreferredSize(new Dimension(40, 40));
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(event -> result.setValue(JOptionPane.CLOSED_OPTION));
        JPanel closeHolder = clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeHolder.add(close);
        top.add(closeHolder, BorderLayout.EAST);

        JPanel words = clear(new BorderLayout(0, 8));
        words.add(label(title, 22, INK), BorderLayout.NORTH);
        words.add(plain("<html><body style='width:300px'>" + message + "</html>", 14, MUTED));

        JButton primary = dark(yes);
        primary.setPreferredSize(new Dimension(100, 50));
        primary.addActionListener(event -> result.setValue(JOptionPane.OK_OPTION));
        JPanel buttons;
        if (no == null) buttons = equal(0, primary);
        else {
            JButton secondary = outline(no);
            secondary.setPreferredSize(new Dimension(100, 50));
            secondary.addActionListener(event -> result.setValue(JOptionPane.NO_OPTION));
            buttons = equal(10, secondary, primary);
        }

        card.add(top, BorderLayout.NORTH);
        card.add(words);
        card.add(buttons, BorderLayout.SOUTH);
        card.add(result, BorderLayout.WEST);

        dialog.setContentPane(card);
        dialog.getRootPane().registerKeyboardAction(event -> result.setValue(JOptionPane.CLOSED_OPTION),
            KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(primary);
        dialog.setPreferredSize(new Dimension(420, no == null ? 270 : 270));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return Integer.valueOf(JOptionPane.OK_OPTION).equals(result.getValue());
    }

    public static JPanel clear(LayoutManager layout) {
        JPanel panel = new SmoothPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** White card with generous padding. */
    public static JPanel card() { return card(Color.WHITE); }

    public static JPanel card(Color fill) {
        RoundedPanel panel = new RoundedPanel(fill);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));
        return panel;
    }

    /** Solid coloured card with the large, softly lighter circle used in the reference. */
    public static RoundedPanel hero(Color fill) {
        RoundedPanel panel = new RoundedPanel(fill);
        panel.decor = fill.equals(INK) ? DARK_CARD_CIRCLE : BRAND_CARD_CIRCLE;
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));
        return panel;
    }

    /** Lays components in a row; each gets the matching share of the width. */
    public static JPanel columns(int gap, double[] weights, Component... items) {
        JPanel row = clear(new GridBagLayout());
        for (int i = 0; i < items.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i; c.gridy = 0; c.weightx = weights[i]; c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, i == 0 ? 0 : gap / 2, 0, i == items.length - 1 ? 0 : gap - gap / 2);
            if (i > 0) c.insets.left = gap - gap / 2;
            row.add(items[i], c);
        }
        return row;
    }

    public static JPanel equal(int gap, Component... items) {
        double[] weights = new double[items.length];
        java.util.Arrays.fill(weights, 1);
        return columns(gap, weights, items);
    }

    public static JButton button(String text) { return solid(text, BRAND, Color.WHITE); }

    public static JButton button(String text, Color fill, Color ink) {
        return fill.equals(Color.WHITE) ? outline(text) : solid(text, fill, ink);
    }

    public static JButton dark(String text) { return solid(text, INK, Color.WHITE); }

    public static JButton solid(String text, Color fill, Color ink) {
        JButton button = new JButton(text);
        button.setBackground(fill);
        button.setForeground(ink);
        styleButton(button);
        return button;
    }

    public static JButton outline(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(INK);
        button.putClientProperty("outline", Boolean.TRUE);
        styleButton(button);
        return button;
    }

    public static JButton link(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setForeground(BRAND);
        button.putClientProperty("ghost", Boolean.TRUE);
        styleButton(button);
        button.setBorder(new EmptyBorder(4, 6, 4, 6));
        return button;
    }

    private static void styleButton(JButton button) {
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(9, 20, 9, 20));
        button.setFont(font(Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(8);
    }


    public static JComponent field(String title, JComponent input) {
        JPanel panel = clear(new BorderLayout(0, 5));
        JLabel caption = new JLabel(title);
        caption.setFont(fontFor(title, Font.PLAIN, 13));
        caption.setForeground(INK);
        panel.add(caption, BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(190, 38));
        panel.add(input);
        return panel;
    }

    public static RoundedField input(String placeholder) {
        RoundedField field = new RoundedField();
        field.setPlaceholder(placeholder);
        return field;
    }

    public static RoundedField search(String placeholder) {
        RoundedField field = new RoundedField();
        field.setPlaceholder(placeholder);
        field.setLeadingIcon(Icons.of(Icons.Kind.SEARCH, MUTED, 17));
        return field;
    }

    /** A grey pill holding mutually exclusive choices; the selected one is filled. */
    public static JPanel segmented(String[] labels, int selected, Color accent, java.util.function.IntConsumer onSelect) {
        RoundedPanel group = new RoundedPanel(SOFT);
        group.radius = 999;
        group.outlined = false;
        group.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        JButton[] buttons = new JButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            JButton button = new JButton(labels[i]);
            buttons[i] = button;
            styleButton(button);
            button.setBorder(new EmptyBorder(6, 16, 6, 16));
            button.setFont(font(Font.PLAIN, 13));
            button.addActionListener(event -> {
                for (int j = 0; j < buttons.length; j++) paintSegment(buttons[j], j == index, accent);
                onSelect.accept(index);
            });
            group.add(button);
        }
        for (int j = 0; j < buttons.length; j++) paintSegment(buttons[j], j == selected, accent);
        return group;
    }

    private static void paintSegment(JButton button, boolean active, Color accent) {
        button.setBackground(active ? accent : new Color(0, 0, 0, 0));
        button.setForeground(active ? Color.WHITE : MUTED);
        button.setFont(font(active ? Font.BOLD : Font.PLAIN, 13));
        button.putClientProperty("ghost", active ? null : Boolean.TRUE);
        button.repaint();
    }

    /** Diagonal green stripes used for revenue and profit throughout the reference. */
    public static Paint hatch(Color base) {
        BufferedImage tile = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pen = tile.createGraphics();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setColor(base);
        pen.fillRect(0, 0, 8, 8);
        pen.setColor(new Color(11, 128, 82));
        pen.setStroke(new BasicStroke(2.6f));
        pen.drawLine(-2, 10, 10, -2);
        pen.drawLine(-2, 2, 2, -2);
        pen.drawLine(6, 10, 10, 6);
        pen.dispose();
        return new TexturePaint(tile, new Rectangle2D.Double(0, 0, 8, 8));
    }

    public static class SmoothPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        public SmoothPanel(LayoutManager layout) { super(layout); }
        protected void paintChildren(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paintChildren(pen);
            pen.dispose();
        }
    }

    public static class RoundedPanel extends SmoothPanel {
        private static final long serialVersionUID = 1L;
        private Color fill;
        public int radius = 30;
        public boolean outlined;
        public boolean clip;
        public Color decor;
        public RoundedPanel(Color fill) {
            super(new FlowLayout());
            this.fill = fill;
            setOpaque(false);
        }
        public void setFill(Color fill) { this.fill = fill; repaint(); }
        private Shape shape() {
            int arc = Math.min(radius * 2, Math.min(getWidth(), getHeight()));
            return new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape shape = shape();
            pen.setColor(fill);
            pen.fill(shape);
            if (decor != null) {
                pen.clip(shape);
                int size = (int) (Math.min(getWidth(), getHeight() * 2.2) * 0.62);
                pen.setColor(decor);
                pen.fillOval(getWidth() - size * 3 / 4, getHeight() - size * 3 / 5, size, size);
            }
            pen.dispose();
            if (outlined) {
                Graphics2D line = (Graphics2D) graphics.create();
                line.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                line.setColor(LINE);
                line.draw(shape);
                line.dispose();
            }
        }
        protected void paintChildren(Graphics graphics) {
            if (!clip) { super.paintChildren(graphics); return; }
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.clip(shape());
            super.paintChildren(pen);
            pen.dispose();
        }
    }

    public static class InputBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        public Insets getBorderInsets(Component component) { return new Insets(8, 16, 8, 16); }
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(8, 16, 8, 16); return insets;
        }
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(LINE);
            pen.draw(new RoundRectangle2D.Double(x + .5, y + .5, width - 1, height - 1, 26, 26));
            pen.dispose();
        }
    }

    /** Text input with a rounded outline, optional leading icon and placeholder. */
    public static class RoundedField extends JTextField {
        private static final long serialVersionUID = 1L;
        private String placeholder = "";
        private Icon leading;
        public RoundedField() {
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(INK);
            setSelectionColor(BRAND_PALE);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setPreferredSize(new Dimension(190, 38));
        }
        public void setPlaceholder(String text) { placeholder = text; repaint(); }
        public void setLeadingIcon(Icon icon) {
            leading = icon;
            setBorder(new EmptyBorder(8, 16 + icon.getIconWidth() + 10, 8, 16));
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(isEnabled() ? Color.WHITE : SOFT);
            pen.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
            pen.setColor(isFocusOwner() ? BRAND : LINE);
            pen.draw(new RoundRectangle2D.Double(.5, .5, getWidth() - 1, getHeight() - 1, 28, 28));
            if (leading != null) leading.paintIcon(this, pen, 16, (getHeight() - leading.getIconHeight()) / 2);
            pen.dispose();
            super.paintComponent(graphics);
            if (getText().isEmpty() && !placeholder.isEmpty()) {
                Graphics2D hint = (Graphics2D) graphics.create();
                hint.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                hint.setColor(new Color(130, 140, 135));
                hint.setFont(getFont());
                Insets in = getInsets();
                hint.drawString(placeholder, in.left, (getHeight() + hint.getFontMetrics().getAscent() - hint.getFontMetrics().getDescent()) / 2);
                hint.dispose();
            }
        }
        { addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent event) { repaint(); }
            public void focusLost(java.awt.event.FocusEvent event) { repaint(); }
        }); }
    }

    public static class RoundedPassword extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private String placeholder = "";
        public RoundedPassword() {
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(INK);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setPreferredSize(new Dimension(190, 38));
            addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent event) { repaint(); }
                public void focusLost(java.awt.event.FocusEvent event) { repaint(); }
            });
        }
        public void setPlaceholder(String text) { placeholder = text; repaint(); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(Color.WHITE);
            pen.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
            pen.setColor(isFocusOwner() ? BRAND : LINE);
            pen.draw(new RoundRectangle2D.Double(.5, .5, getWidth() - 1, getHeight() - 1, 28, 28));
            pen.dispose();
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !placeholder.isEmpty()) {
                Graphics2D hint = (Graphics2D) graphics.create();
                hint.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                hint.setColor(new Color(130, 140, 135));
                hint.setFont(getFont());
                Insets in = getInsets();
                hint.drawString(placeholder, in.left, (getHeight() + hint.getFontMetrics().getAscent() - hint.getFontMetrics().getDescent()) / 2);
                hint.dispose();
            }
        }
    }

    /** Pill-shaped status badge with a leading dot, coloured by meaning. */
    public static class Chip extends JComponent {
        private static final long serialVersionUID = 1L;
        private String text;
        private Color background, foreground, dot;
        private boolean showDot = true;
        public Chip(String text) { setText(text); setOpaque(false); }
        public Chip(String text, Color background, Color foreground, Color dot) {
            this.text = text; this.background = background; this.foreground = foreground; this.dot = dot;
            setOpaque(false);
        }
        public void setShowDot(boolean value) { showDot = value; }
        public void setText(String value) {
            text = value;
            switch (value) {
                case "Revenue": case "Created": case "Active": case "Scheduled": case "Paid": case "Upcoming":
                    background = MINT; foreground = BRAND; dot = BRAND; break;
                case "Admin": case "Edited": case "Deleted":
                    background = INK; foreground = Color.WHITE; dot = Color.WHITE; break;
                default:
                    background = SOFT; foreground = new Color(60, 68, 64); dot = new Color(60, 68, 64);
            }
            if (value.startsWith("Due")) { background = INK; foreground = Color.WHITE; dot = Color.WHITE; }
            revalidate(); repaint();
        }
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(font(Font.BOLD, 12));
            return new Dimension(metrics.stringWidth(text) + (showDot ? 38 : 26), 26);
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setColor(background);
            pen.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            int x = 13;
            if (showDot) { pen.setColor(dot); pen.fillOval(x, getHeight() / 2 - 3, 6, 6); x += 12; }
            pen.setColor(foreground);
            pen.setFont(font(Font.BOLD, 12));
            FontMetrics metrics = pen.getFontMetrics();
            pen.drawString(text, x, (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
            pen.dispose();
        }
    }

    /** Circle with the user's initials: dark for admins, mint for everyone else. */
    public static class Avatar extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String initials;
        private final boolean dark;
        private final int size;
        public Avatar(String name, boolean dark, int size) {
            String clean = name == null ? "" : name.trim();
            initials = clean.length() >= 2 ? clean.substring(0, 2).toUpperCase(Locale.ROOT) : clean.toUpperCase(Locale.ROOT);
            this.dark = dark; this.size = size; setOpaque(false);
        }
        public Dimension getPreferredSize() { return new Dimension(size, size); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setColor(dark ? INK : MINT);
            pen.fillOval(0, 0, getWidth(), getHeight());
            pen.setColor(dark ? Color.WHITE : BRAND);
            pen.setFont(font(Font.BOLD, size * 0.36f));
            FontMetrics metrics = pen.getFontMetrics();
            pen.drawString(initials, (getWidth() - metrics.stringWidth(initials)) / 2,
                (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
            pen.dispose();
        }
    }

    /** Filled circle holding a line icon, used as the corner badge on stat cards. */
    public static JComponent badge(Icons.Kind kind, Color circle, Color stroke, int size) {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); }
            public Dimension getPreferredSize() { return new Dimension(size, size); }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setColor(circle);
                pen.fillOval(0, 0, size - 1, size - 1);
                Icon icon = Icons.of(kind, stroke, size * 5 / 12);
                icon.paintIcon(this, pen, (size - icon.getIconWidth()) / 2, (size - icon.getIconHeight()) / 2);
                pen.dispose();
            }
        };
    }
}

```

