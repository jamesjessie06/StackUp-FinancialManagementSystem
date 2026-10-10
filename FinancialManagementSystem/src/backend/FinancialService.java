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
        ArrayList<Account> pending = new ArrayList<>();
    }

    private FinancialService(Path file) throws IOException {
        this.file = file;
        state = new State();
        if (Files.exists(file))
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
                in.setObjectInputFilter(
                    ObjectInputFilter.Config.createFilter("java.base/*;backend.*;!*"));
                state = (State) in.readObject();
                if (state.pending == null) state.pending = new ArrayList<>();
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
        nextState.pending.addAll(state.pending);
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
        checkUsernameFree(username);
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        State nextState = copy();
        nextState.accounts.add(
            new Account(username, role, encodedSalt, hash(password, encodedSalt)));
        save(nextState);
    }

    private void checkUsernameFree(String username) {
        for (Account account : state.accounts)
            if (account.getUsername().equalsIgnoreCase(username))
                throw new IllegalArgumentException("Username already exists.");
        for (Account account : state.pending)
            if (account.getUsername().equalsIgnoreCase(username))
                throw new IllegalArgumentException("That username is already waiting for approval.");
    }

    /** Anyone can request an account; it has no role and cannot sign in until an admin approves it. */
    public void requestAccount(String username, char[] password) throws Exception {
        username = username.trim();
        if (username.isBlank() || password.length < 8)
            throw new IllegalArgumentException(
                "Enter a username and a password of at least 8 characters.");
        if (!hasAccounts())
            throw new IllegalArgumentException("The first account must be created as Admin.");
        checkUsernameFree(username);
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        State nextState = copy();
        nextState.pending.add(
            new Account(username, "Pending", encodedSalt, hash(password, encodedSalt)));
        save(nextState);
    }

    public List<Account> getPendingAccounts() {
        require("Admin");
        return List.copyOf(state.pending);
    }

    /** The reviewing admin decides whether the approved account is an Employee or an Admin. */
    public void approveAccount(String username, String role) throws IOException {
        require("Admin");
        if (!role.equals("Admin") && !role.equals("Employee"))
            throw new IllegalArgumentException("Invalid role.");
        State nextState = copy();
        Account found = takePending(nextState, username);
        nextState.accounts.add(
            new Account(found.getUsername(), role, found.getSalt(), found.getHash()));
        log(nextState, "Approved new " + role + " account | " + found.getUsername());
        save(nextState);
    }

    public void rejectAccount(String username) throws IOException {
        require("Admin");
        State nextState = copy();
        Account found = takePending(nextState, username);
        log(nextState, "Rejected account request | " + found.getUsername());
        save(nextState);
    }

    private static Account takePending(State nextState, String username) {
        for (int index = 0; index < nextState.pending.size(); index++)
            if (nextState.pending.get(index).getUsername().equalsIgnoreCase(username))
                return nextState.pending.remove(index);
        throw new IllegalArgumentException("That request no longer exists.");
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
        for (Account account : state.pending)
            if (account.getUsername().equalsIgnoreCase(username.trim())
                && MessageDigest.isEqual(Base64.getDecoder().decode(account.getHash()),
                    Base64.getDecoder().decode(hash(password, account.getSalt()))))
                throw new IllegalArgumentException(
                    "Your account is still waiting for an admin to approve it.");
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
