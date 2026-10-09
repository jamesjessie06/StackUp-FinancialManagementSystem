## src/Main.java

```java
import backend.FinancialService;
import frontend.LoginUI;
import frontend.Theme;
import javax.swing.*;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.install();
                new LoginUI(FinancialService.getInstance()).setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,exception.getMessage(),"Startup error",JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

```

## src/backend/Account.java

```java
package backend;

import java.io.Serializable;
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
                int series = record instanceof Revenue ? 0 : 1;
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
/** A expense transaction inherits the common financial fields. */
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
}

```

## src/backend/FinancialRecord.java

```java
package backend;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
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

    public abstract String getType();
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

    public Payment(String description, String category, BigDecimal amount, LocalDate due) {
        this(UUID.randomUUID().toString(), description, category, amount, due, false);
    }

    private Payment(String id, String description, String category, BigDecimal amount,
        LocalDate due, boolean paid) {
        if (description.isBlank() || category.isBlank() || amount.signum() <= 0
            || amount.scale() > 2)
            throw new IllegalArgumentException(
                "Description, category and a positive amount are required.");
        this.description = description.trim();
        this.category = category.trim();
        this.amount = amount;
        this.due = java.util.Objects.requireNonNull(due);
        this.paid = paid;
        this.id = id;
    }

    public Payment asPaid() {
        return new Payment(id, description, category, amount, due, true);
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
/** A revenue transaction inherits the common financial fields. */
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
}

```

## src/frontend/AccentPanel.java

```java
package frontend;

import java.awt.*;

/** Decorative shapes give the interface depth without adding financial data. */
public final class AccentPanel extends Theme.SmoothPanel {
    private static final long serialVersionUID = 1L;
    private final boolean dark;

    public AccentPanel(LayoutManager layout, boolean dark) {
        super(layout);
        this.dark = dark;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color start = dark ? new Color(0, 93, 88) : new Color(240, 249, 246);
        Color end = dark ? new Color(0, 163, 142) : new Color(249, 251, 253);
        pen.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
        pen.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
        pen.setColor(dark ? new Color(255, 255, 255, 18) : new Color(0, 151, 139, 12));
        int size = Math.max(160, getHeight() * 2);
        pen.fillOval(getWidth() - size / 2, -size / 2, size, size);
        pen.setStroke(new BasicStroke(22));
        pen.drawOval(getWidth() - size / 2 - 80, -size / 2 + 60, size, size);
        pen.dispose();
    }
}

```

## src/frontend/AccountsUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class AccountsUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public AccountsUI(DashboardUI window) {
        super(window);
        JTextField username=new JTextField();JPasswordField password=new JPasswordField();
        JComboBox<String> role=new JComboBox<>(new String[]{"Employee","Admin"});
        JButton save=Theme.button("Save Account");
        JPanel form=Theme.clear(new GridLayout(4,1,0,12));
        form.add(Theme.field("Username",username));form.add(Theme.field("Password (8+ characters)",password));form.add(Theme.field("Role",role));form.add(save);
        JPanel create=Theme.clear(new BorderLayout(0,18));create.add(form,BorderLayout.NORTH);
        JPanel rolePreview=new AccentPanel(new BorderLayout(0,10),false);
        rolePreview.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        rolePreview.setPreferredSize(new Dimension(230,112));
        rolePreview.add(Theme.label("Account access",13,Theme.TEAL),BorderLayout.NORTH);
        JLabel permission=new JLabel("<html>Employee<br><br>Record transactions and manage payments.</html>");
        permission.setForeground(Theme.MUTED);
        rolePreview.add(permission);
        role.addActionListener(event -> permission.setText(role.getSelectedItem().equals("Admin")
            ? "<html>Admin<br><br>Review reports, manage records and accounts.</html>"
            : "<html>Employee<br><br>Record transactions and manage payments.</html>"));
        create.add(rolePreview,BorderLayout.SOUTH);
        JPanel left=titled("Create Account",create);left.setPreferredSize(new Dimension(285,400));add(left,BorderLayout.WEST);
        DefaultTableModel rows=model("Username","Role");JTable accounts=table(rows);
        Runnable refresh=()->{rows.setRowCount(0);for(Account account:service.getAccounts())rows.addRow(new Object[]{account.getUsername(),account.getRole()});};refresh.run();
        save.addActionListener(event->{char[] value=password.getPassword();try{service.createAccount(username.getText(),value,(String)role.getSelectedItem());username.setText("");refresh.run();JOptionPane.showMessageDialog(this,"Account created.");}catch(Exception exception){error(exception);}finally{Arrays.fill(value,'\0');password.setText("");}});
        JTextField selected=new JTextField();selected.setEditable(false);JButton reset=Theme.button("Reset Employee Password");reset.setEnabled(false);
        accounts.getSelectionModel().addListSelectionListener(event->{int row=accounts.getSelectedRow();selected.setText(row>=0?rows.getValueAt(row,0).toString():"");reset.setEnabled(row>=0&&"Employee".equals(rows.getValueAt(row,1)));});
        reset.addActionListener(event->{JPasswordField replacement=new JPasswordField();if(JOptionPane.showConfirmDialog(this,Theme.field("New password",replacement),"Reset Employee Password",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){char[] value=replacement.getPassword();try{service.resetEmployeePassword(selected.getText(),value);JOptionPane.showMessageDialog(this,"Password updated.");}catch(Exception exception){error(exception);}finally{Arrays.fill(value,'\0');replacement.setText("");}}});
        JPanel right=Theme.clear(new BorderLayout(0,12));right.add(titled("Registered Accounts",tableArea(accounts,"No registered accounts.")));
        JPanel resetForm=Theme.clear(new BorderLayout(0,10));resetForm.add(selected);resetForm.add(reset,BorderLayout.SOUTH);
        JPanel resetCard=titled("Reset Employee Password",resetForm);resetCard.setPreferredSize(new Dimension(400,135));right.add(resetCard,BorderLayout.SOUTH);add(right);
    }
}

```

## src/frontend/ActivityLogUI.java

```java
package frontend;

import javax.swing.table.DefaultTableModel;

public final class ActivityLogUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public ActivityLogUI(DashboardUI window) {
        super(window);DefaultTableModel rows=model("Action","User","Details","Date");
        for(String entry:service.getActivity()) {
            String[] parts=entry.split(" \\| ",3);
            if(parts.length==3) {
                String[] action=parts[2].split(": ",2);
                rows.addRow(new Object[]{action[0],parts[1],action.length>1?action[1]:"",parts[0].split("T")[0]});
            }
        }
        add(tableArea(table(rows),"No activity yet."));
    }
}

```

## src/frontend/ChartPanel.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** Real-data charts: decorative mockup bars are never saved as transactions. */
public final class ChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public enum Mode { CASH_FLOW, MONTHLY, TOTALS, REVENUE_CATEGORIES, EXPENSE_CATEGORIES }
    private final List<FinancialRecord> records;
    private final Mode mode;
    private final int year;
    private final Color[] colors={Theme.GREEN,Theme.RED,Theme.BLUE};
    public ChartPanel(List<FinancialRecord> records, Mode mode, int year) {
        this.records=List.copyOf(records); this.mode=mode; this.year=year; setOpaque(false);
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D pen=(Graphics2D)graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setFont(new Font("Nimbus Sans",Font.PLAIN,10));
        if (mode==Mode.REVENUE_CATEGORIES || mode==Mode.EXPENSE_CATEGORIES) donut(pen);
        else if(mode==Mode.CASH_FLOW) cashFlow(pen);
        else bars(pen);
        pen.dispose();
    }
    /** Stacked monthly inflow/outflow bars; the stack represents total activity, not profit. */
    private void cashFlow(Graphics2D pen) {
        BigDecimal[][] data=ChartData.monthly(records,year);
        double maximum=1;
        for(int month=0;month<12;month++)maximum=Math.max(maximum,data[0][month].doubleValue()+data[1][month].doubleValue());
        String[] names={"Revenue","Expenses"};Color[] palette={Theme.TEAL,Theme.BLACK};
        for(int i=0;i<2;i++){pen.setColor(palette[i]);pen.fillRoundRect(8+i*95,3,7,7,3,3);pen.setColor(Theme.MUTED);pen.drawString(names[i],20+i*95,10);}
        int left=40,right=getWidth()-6,top=28,bottom=getHeight()-25;
        for(int i=0;i<5;i++){int y=top+(bottom-top)*i/4;pen.setColor(Theme.LINE);pen.drawLine(left,y,right,y);pen.setColor(Theme.MUTED);pen.drawString(String.format(java.util.Locale.US,maximum<=1?"%.2f":"%.0f",maximum*(4-i)/4),0,y+3);}
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for(int m=0;m<12;m++){
            int x=left+(int)((right-left)*(m+.5)/12);int width=Math.max(4,(right-left)/12-7);int base=bottom;
            for(int part=1;part>=0;part--){int h=(int)(data[part][m].doubleValue()/maximum*(bottom-top));if(h>0){pen.setColor(palette[part]);pen.fillRoundRect(x-width/2,base-h,width,Math.max(1,h-2),5,5);base-=h;}}
            pen.setColor(Theme.MUTED);center(pen,months[m],x,bottom+16);
        }
        if(records.isEmpty()){pen.setColor(Theme.MUTED);center(pen,"No transactions recorded yet",(left+right)/2,(top+bottom)/2);}
    }
    private void donut(Graphics2D pen) {
        String type=mode==Mode.REVENUE_CATEGORIES ? "Revenue" : "Expense";
        Map<String,BigDecimal> data=ChartData.categories(records,type);
        BigDecimal total=ChartData.total(records,type);
        int diameter=Math.max(40,Math.min(getHeight()-35,getWidth()/2));
        int x=data.isEmpty() ? (getWidth()-diameter)/2 : 4;
        int y=(getHeight()-diameter)/2;
        pen.setColor(Theme.LINE); pen.fillOval(x,y,diameter,diameter);
        int angle=90,index=0;
        for (Map.Entry<String,BigDecimal> entry:data.entrySet()) {
            double share=entry.getValue().doubleValue()/total.doubleValue();
            int arc=(int)Math.round(share*360);
            pen.setColor(colors[index%3]); pen.fillArc(x,y,diameter,diameter,angle,arc);
            int ly=18+index*27;
            pen.fillOval(diameter+18,ly-7,7,7); pen.setColor(Theme.MUTED);
            String name=entry.getKey(); if(name.length()>16) name=name.substring(0,15)+"…";
            pen.drawString(name,diameter+31,ly);
            pen.drawString(String.format(java.util.Locale.US,"%.1f%%",share*100),diameter+31,ly+12);
            angle+=arc; index++;
        }
        pen.setColor(Color.WHITE);
        int hole=(int)(diameter*.68); pen.fillOval(x+(diameter-hole)/2,y+(diameter-hole)/2,hole,hole);
        pen.setColor(Theme.INK); center(pen,Theme.money(total),x+diameter/2,y+diameter/2);
        if(data.isEmpty()) { pen.setColor(Theme.MUTED); center(pen,"No records yet",getWidth()/2,getHeight()-4); }
    }
    private void bars(Graphics2D pen) {
        String[] names={"Revenue","Expenses","Profit"};
        for(int s=0;s<3;s++) {
            pen.setColor(colors[s]); pen.fillOval(10+s*92,4,6,6);
            pen.setColor(Theme.MUTED); pen.drawString(names[s],20+s*92,10);
        }
        BigDecimal[][] monthly=ChartData.monthly(records,year);
        double[] totals={ChartData.total(records,"Revenue").doubleValue(),ChartData.total(records,"Expense").doubleValue(),0};
        totals[2]=totals[0]-totals[1];
        double max=records.isEmpty()?1:1,min=0;
        if(mode==Mode.MONTHLY) for(BigDecimal[] series:monthly) for(BigDecimal value:series) {
            max=Math.max(max,value.doubleValue()); min=Math.min(min,value.doubleValue());
        }
        else for(double value:totals) { max=Math.max(max,value); min=Math.min(min,value); }
        int left=32,right=getWidth()-10,top=28,bottom=getHeight()-24;
        for(int i=0;i<5;i++) {
            int y=top+(bottom-top)*i/4; pen.setColor(Theme.LINE); pen.drawLine(left,y,right,y);
            pen.setColor(Theme.MUTED);
            pen.drawString(String.format(java.util.Locale.US,max<=1?"%.2f":"%.0f",max-(max-min)*i/4),0,y+3);
        }
        int zero=position(0,min,max,top,bottom);
        int groups=mode==Mode.MONTHLY?12:3;
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for(int group=0;group<groups;group++) {
            int center=left+(int)((right-left)*(group+.5)/groups);
            int width=Math.max(3,Math.min(15,(right-left)/groups/4));
            for(int series=0;series<(mode==Mode.MONTHLY?3:1);series++) {
                double value=mode==Mode.MONTHLY?monthly[series][group].doubleValue():totals[group];
                int y=position(value,min,max,top,bottom);
                pen.setColor(colors[mode==Mode.MONTHLY?series:group]);
                if(value!=0) pen.fillRoundRect(center+(mode==Mode.MONTHLY?(series-1)*width:-width),
                    Math.min(zero,y),mode==Mode.MONTHLY?width-1:width*2,Math.max(1,Math.abs(zero-y)),6,6);
            }
            pen.setColor(Theme.MUTED); center(pen,mode==Mode.MONTHLY?months[group]:names[group],center,bottom+17);
        }
        if(records.isEmpty()) {
            int cx=(left+right)/2,cy=(top+bottom)/2;
            pen.setColor(new Color(238,249,243));
            pen.fillRoundRect(cx-105,cy-27,210,54,18,18);
            pen.setColor(Theme.TEAL);
            center(pen,"Your chart starts with your data",cx,cy-3);
            pen.setColor(Theme.MUTED);
            center(pen,"No transactions recorded yet",cx,cy+13);
        }
    }
    private int position(double value,double min,double max,int top,int bottom) {
        return bottom-(int)((value-min)/(max-min)*(bottom-top));
    }
    private void center(Graphics2D pen,String text,int x,int y) {
        Font original=pen.getFont();
        if(text.contains("₱"))pen.setFont(new Font("DejaVu Sans",original.getStyle(),original.getSize()));
        pen.drawString(text,x-pen.getFontMetrics().stringWidth(text)/2,y);
        pen.setFont(original);
    }
}

```

## src/frontend/ComboBoxSkin.java

```java
package frontend;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Rounded white combo box without Metal's nested editor border. */
public final class ComboBoxSkin extends BasicComboBoxUI {
    public static ComponentUI createUI(JComponent component) { return new ComboBoxSkin(); }
    protected JButton createArrowButton() {
        JButton arrow=Theme.button("⌄",Color.WHITE,Theme.MUTED);
        arrow.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        return arrow;
    }
    protected ListCellRenderer<Object> createRenderer() {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID=1L;
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                label.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
                label.setBackground(selected?new Color(221,247,240):Color.WHITE);
                label.setForeground(Theme.INK);return label;
            }
        };
    }
}

```

## src/frontend/DashboardPage.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Reference-inspired card grid showing only actual system data. */
public final class DashboardPage extends PageSupport {
    private static final long serialVersionUID = 1L;
    public DashboardPage(DashboardUI window) {
        super(window);
        setLayout(new BorderLayout(14,14));
        BigDecimal revenue=service.total("Revenue",LocalDate.MIN,LocalDate.MAX);
        BigDecimal expense=service.total("Expense",LocalDate.MIN,LocalDate.MAX);
        JPanel left=Theme.clear(new BorderLayout(0,14));left.setPreferredSize(new Dimension(275,480));
        JPanel balance=metric("Net balance",revenue.subtract(expense),false);
        JPanel actions=Theme.clear(new GridLayout(1,2,8,0));
        JButton history=Theme.button("History",Theme.BLACK,Color.WHITE);history.addActionListener(e->window.navigate("Transaction History"));actions.add(history);
        JButton next=Theme.button(admin?"Reports":"Record",new Color(243,244,244),Theme.BLACK);next.addActionListener(e->window.navigate(admin?"Financial Report":"Financial Record"));actions.add(next);balance.add(actions,BorderLayout.SOUTH);
        balance.setPreferredSize(new Dimension(275,190));left.add(balance,BorderLayout.NORTH);
        JPanel payments=titled("Upcoming payments",emptyState("No pending payments","Scheduled items appear here.",4));
        long pending=service.getPayments().stream().filter(v->!v.isPaid()).count();
        if(pending>0)payments=titled("Upcoming payments",Theme.label(pending+" pending payment(s)",20,Theme.TEAL));
        left.add(payments);
        JPanel status=new AccentPanel(new BorderLayout(0,12),true);status.setBorder(BorderFactory.createEmptyBorder(20,18,20,18));status.add(Theme.label("StackUp",21,Color.WHITE),BorderLayout.NORTH);status.add(Theme.label(service.getRecords().size()+" transactions recorded",13,Color.WHITE));status.setPreferredSize(new Dimension(275,125));left.add(status,BorderLayout.SOUTH);add(left,BorderLayout.WEST);
        JPanel main=Theme.clear(new BorderLayout(14,14));JPanel upper=Theme.clear(new GridLayout(1,2,14,0));upper.setPreferredSize(new Dimension(700,270));
        JPanel metrics=Theme.card();metrics.setLayout(new GridLayout(2,2,10,10));
        metrics.add(metric("Total revenue",revenue,true));metrics.add(metric("Total expenses",expense,false));metrics.add(metric("Total profit",revenue.subtract(expense),false));metrics.add(metric("Today's revenue",service.total("Revenue",LocalDate.now(),LocalDate.now()),false));upper.add(metrics);
        upper.add(titled("Monthly cash flow",new ChartPanel(service.getRecords(),ChartPanel.Mode.CASH_FLOW,LocalDate.now().getYear())));main.add(upper,BorderLayout.NORTH);
        DefaultTableModel recent=model("Description","Category","Amount","Date");java.util.List<FinancialRecord> records=service.getRecords();addRows(recent,records.subList(Math.max(0,records.size()-5),records.size()));main.add(titled("Recent transactions",tableArea(table(recent),"No transactions recorded yet.")));add(main);
    }
    private JPanel metric(String title,BigDecimal value,boolean accent){
        JPanel card=new Theme.RoundedPanel(accent?Theme.TEAL:new Color(246,246,246));card.setLayout(new BorderLayout(0,12));card.setBorder(BorderFactory.createEmptyBorder(14,12,14,12));
        JPanel heading=Theme.clear(new BorderLayout());JLabel name=new JLabel(title);name.setFont(new Font("Nimbus Sans",Font.PLAIN,11));name.setForeground(accent?Color.WHITE:Theme.BLACK);heading.add(name);heading.add(new JLabel(Theme.icon(1,accent?Color.WHITE:Theme.MUTED,false)),BorderLayout.EAST);card.add(heading,BorderLayout.NORTH);card.add(Theme.label(Theme.money(value),21,accent?Color.WHITE:Theme.BLACK));return card;
    }
}

```

## src/frontend/DashboardUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/** Fresh desktop shell built from the approved mockup. */
public final class DashboardUI extends JFrame {
    private static final long serialVersionUID=1L;
    private final FinancialService service;
    private final boolean admin;
    private final JPanel workspace=Theme.clear(new BorderLayout(0,14));
    private final Map<String,JButton> menu=new LinkedHashMap<>();
    private String current="Dashboard";
    public DashboardUI(FinancialService service) {
        super("StackUp Financial Management System");this.service=service;admin=service.getCurrent().getRole().equals("Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1200,730);setMinimumSize(new Dimension(1040,650));setLocationRelativeTo(null);
        JPanel outer=new JPanel(new BorderLayout());outer.setBackground(new Color(233,233,233));
        outer.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));setContentPane(outer);
        JPanel root=new Theme.RoundedPanel(Theme.BACKGROUND);root.setLayout(new BorderLayout(16,18));
        root.setBorder(BorderFactory.createEmptyBorder(16,14,16,14));outer.add(root);
        String[] pages=admin?new String[]{"Dashboard","Transaction History","Financial Report","Financial Insights","Activity Log","Accounts"}:new String[]{"Dashboard","Financial Record","Transaction History","Payments"};
        JPanel top=Theme.clear(new BorderLayout(22,0));
        JLabel brand=Theme.label("StackUp",17,Theme.BLACK);brand.setIcon(Theme.icon(2,Theme.TEAL,true));top.add(brand,BorderLayout.WEST);
        JPanel tabs=new Theme.RoundedPanel(Color.WHITE);tabs.setLayout(new FlowLayout(FlowLayout.CENTER,3,3));
        for(String page:pages){JButton button=Theme.button(page,Color.WHITE,Theme.MUTED);button.setFont(new Font("Nimbus Sans",Font.PLAIN,10));button.addActionListener(event->open(page));menu.put(page,button);tabs.add(button);}
        top.add(tabs);
        JLabel account=Theme.label(service.getCurrent().getUsername()+"  ·  "+service.getCurrent().getRole(),11,Theme.BLACK);account.setIcon(Theme.icon(3,Theme.TEAL,true));top.add(account,BorderLayout.EAST);root.add(top,BorderLayout.NORTH);
        JPanel sidebar=new Theme.RoundedPanel(Color.WHITE);sidebar.setPreferredSize(new Dimension(44,550));sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));sidebar.setBorder(BorderFactory.createEmptyBorder(14,3,10,3));
        int index=0;
        for(String page:pages){JButton icon=Theme.button("",Color.WHITE,Theme.MUTED);icon.setIcon(Theme.icon(index++,Theme.BLACK,false));icon.setToolTipText(page);icon.setMaximumSize(new Dimension(38,40));icon.addActionListener(event->open(page));sidebar.add(icon);sidebar.add(Box.createVerticalStrut(6));}
        sidebar.add(Box.createVerticalGlue());JButton logout=Theme.button("↪",Color.WHITE,Theme.BLACK);logout.setToolTipText("Logout");logout.setMaximumSize(new Dimension(38,40));sidebar.add(logout);
        logout.addActionListener(event->{if(JOptionPane.showConfirmDialog(this,"Log out?","Logout",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){service.logout();new LoginUI(service).setVisible(true);dispose();}});
        root.add(sidebar,BorderLayout.WEST);root.add(workspace);showPage("Dashboard");
    }
    public FinancialService service() { return service; }
    public void refresh() { showPage(current); }
    public void navigate(String page) { open(page); }
    private void open(String page) {
        if(current.equals(page)) JOptionPane.showMessageDialog(this,page+" is already open.","Page Already Open",JOptionPane.WARNING_MESSAGE);
        else showPage(page);
    }
    private void showPage(String page) {
        current=page;workspace.removeAll();for(Map.Entry<String,JButton> entry:menu.entrySet())entry.getValue().setBackground(entry.getKey().equals(page)?Theme.BLACK:Color.WHITE);
        for(Map.Entry<String,JButton> entry:menu.entrySet())entry.getValue().setForeground(entry.getKey().equals(page)?Color.WHITE:Theme.MUTED);
        JPanel header=Theme.clear(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));header.add(Theme.label(page.equals("Dashboard")?"Good day, "+service.getCurrent().getUsername():page,26,Theme.BLACK),BorderLayout.WEST);
        workspace.add(header,BorderLayout.NORTH);
        JPanel screen;
        switch(page) {
            case "Financial Record":screen=new FinancialRecordUI(this);break;
            case "Transaction History":screen=new TransactionHistoryUI(this);break;
            case "Financial Report":screen=new FinancialReportUI(this);break;
            case "Financial Insights":screen=new FinancialInsightsUI(this);break;
            case "Activity Log":screen=new ActivityLogUI(this);break;
            case "Accounts":screen=new AccountsUI(this);break;
            case "Payments":screen=new PaymentsUI(this);break;
            default:screen=new DashboardPage(this);
        }
        workspace.add(screen);workspace.revalidate();workspace.repaint();
    }
}

```

## src/frontend/DatePicker.java

```java
package frontend;

import java.awt.*;
import java.time.*;
import javax.swing.*;

/** Read-only input backed by a Swing calendar. */
public final class DatePicker extends JTextField {
    private static final long serialVersionUID = 1L;
    private LocalDate selected;
    private YearMonth month = YearMonth.now();
    public DatePicker() {
        setEditable(false);
        setToolTipText("Choose a date");
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
        setText(date == null ? "" : date.toString());
        if (date != null) month = YearMonth.from(date);
    }
    private void open() {
        JPopupMenu popup = new JPopupMenu();
        JPanel calendar = Theme.card();
        popup.add(calendar);
        rebuild(calendar, popup);
        popup.show(this, 0, getHeight());
    }
    private void rebuild(JPanel calendar, JPopupMenu popup) {
        calendar.removeAll();
        calendar.setLayout(new BorderLayout(0, 8));
        JPanel header = Theme.clear(new BorderLayout());
        JButton previous = Theme.button("‹"), next = Theme.button("›");
        header.add(previous, BorderLayout.WEST);
        header.add(new JLabel(month.toString(), SwingConstants.CENTER));
        header.add(next, BorderLayout.EAST);
        previous.addActionListener(event -> { month=month.minusMonths(1); rebuild(calendar,popup); });
        next.addActionListener(event -> { month=month.plusMonths(1); rebuild(calendar,popup); });
        calendar.add(header, BorderLayout.NORTH);
        JPanel days = Theme.clear(new GridLayout(7,7,3,3));
        for (String name : new String[]{"Mo","Tu","We","Th","Fr","Sa","Su"})
            days.add(new JLabel(name, SwingConstants.CENTER));
        int offset=month.atDay(1).getDayOfWeek().getValue()-1;
        for (int cell=0; cell<42; cell++) {
            int number=cell-offset+1;
            if (number<1 || number>month.lengthOfMonth()) { days.add(new JLabel()); continue; }
            LocalDate date=month.atDay(number);
            JButton day=Theme.button(Integer.toString(number),
                date.equals(selected) ? Theme.TEAL : Color.WHITE,
                date.equals(selected) ? Color.WHITE : Theme.INK);
            day.setBorder(BorderFactory.createEmptyBorder(5,4,5,4));
            day.setPreferredSize(new Dimension(34,29));
            day.addActionListener(event -> { setSelectedDate(date); popup.setVisible(false); });
            days.add(day);
        }
        calendar.add(days);
        calendar.revalidate(); calendar.repaint(); popup.pack();
    }
}

```

## src/frontend/FinancialInsightsUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public final class FinancialInsightsUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public FinancialInsightsUI(DashboardUI window) {
        super(window);
        JPanel top=Theme.clear(new GridLayout(1,2,12,0));top.setPreferredSize(new Dimension(700,65));
        for(String type:new String[]{"Revenue","Expense"}) {
            Color color=type.equals("Revenue")?Theme.GREEN:Theme.RED;
            JPanel highlight=new Theme.RoundedPanel(type.equals("Revenue")?new Color(222,247,232):new Color(255,226,232));
            highlight.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));highlight.setLayout(new GridLayout(2,1));
            highlight.add(Theme.label("Highest "+type,12,color));
            FinancialRecord highest=null;
            for(FinancialRecord record:service.getRecords())if(record.getType().equals(type)&&(highest==null||record.getAmount().compareTo(highest.getAmount())>0))highest=record;
            JLabel detail=new JLabel(highest==null?"No records yet.":highest.getDescription()+" — "+Theme.money(highest.getAmount()));detail.setForeground(color);highlight.add(detail);top.add(highlight);
        }
        add(top,BorderLayout.NORTH);
        int year=LocalDate.now().getYear();
        JPanel body=Theme.clear(new GridLayout(2,1,12,12));
        JPanel categories=Theme.clear(new GridLayout(1,2,12,0));
        categories.add(titled("Revenue by Category",new ChartPanel(service.getRecords(),ChartPanel.Mode.REVENUE_CATEGORIES,year)));
        categories.add(titled("Expense by Category",new ChartPanel(service.getRecords(),ChartPanel.Mode.EXPENSE_CATEGORIES,year)));
        body.add(categories);body.add(titled("Monthly Performance",new ChartPanel(service.getRecords(),ChartPanel.Mode.MONTHLY,year)));add(body);
    }
}

```

## src/frontend/FinancialRecordUI.java

```java
package frontend;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class FinancialRecordUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public FinancialRecordUI(DashboardUI window) {
        super(window);
        JPanel actions=Theme.clear(new GridLayout(1,2,12,0)); actions.setPreferredSize(new Dimension(760,152));
        for(String type:new String[]{"Revenue","Expense"}) {
            Color color=type.equals("Revenue")?Theme.GREEN:Theme.RED;
            Color pale=type.equals("Revenue")?new Color(222,247,232):new Color(255,226,232);
            JButton add=Theme.button("Add "+type,pale,color);
            add.setFont(new Font("Nimbus Sans",Font.BOLD,20));
            add.setIcon(Theme.icon(3,color,true)); add.setIconTextGap(12);
            add.setHorizontalAlignment(SwingConstants.LEFT);
            add.setBorder(BorderFactory.createEmptyBorder(15,22,15,22));
            add.addActionListener(event->record(type,null)); actions.add(add);
        }
        add(actions,BorderLayout.NORTH);
        DefaultTableModel recent=model("Description","Category","Amount","Date"); addRows(recent,service.getRecords());
        add(titled("Recent Records",tableArea(table(recent),"No records yet.")));
    }
}

```

## src/frontend/FinancialReportUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public final class FinancialReportUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public FinancialReportUI(DashboardUI window) {
        super(window);
        DatePicker from=new DatePicker(),to=new DatePicker(); from.setSelectedDate(LocalDate.now());to.setSelectedDate(LocalDate.now());
        JButton generate=Theme.button("Generate Report");
        JPanel top=Theme.clear(new BorderLayout(0,12));
        JPanel filters=Theme.clear(new GridLayout(1,3,12,0));
        filters.add(Theme.field("From date",from)); filters.add(Theme.field("To date",to));filters.add(Theme.field(" ",generate));
        top.add(filters,BorderLayout.NORTH);
        JPanel totals=Theme.clear(new BorderLayout());top.add(totals);add(top,BorderLayout.NORTH);
        JPanel charts=Theme.clear(new GridLayout(1,2,12,0));add(charts);
        Runnable update=()->{
            try {
                LocalDate start=from.getSelectedDate(),end=to.getSelectedDate();
                if(start.isAfter(end))throw new IllegalArgumentException("From date must be before To date.");
                java.util.List<FinancialRecord> records=ChartData.between(service.getRecords(),start,end);
                totals.removeAll();totals.add(summary(start,end,false));charts.removeAll();
                charts.add(titled("Revenue vs Expenses vs Profit",new ChartPanel(records,ChartPanel.Mode.TOTALS,start.getYear())));
                charts.add(titled("Expense Category Breakdown",new ChartPanel(records,ChartPanel.Mode.EXPENSE_CATEGORIES,start.getYear())));
                revalidate();repaint();
            }catch(Exception exception){error(exception);}
        };
        generate.addActionListener(event->update.run());update.run();
    }
}

```

## src/frontend/LoginUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Split login: an illustrated teal introduction beside the working sign-in form. */
public final class LoginUI extends JFrame {
    private static final long serialVersionUID = 1L;

    public LoginUI(FinancialService service) {
        super("StackUp Financial Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 480);
        setMinimumSize(new Dimension(740, 450));
        setLocationRelativeTo(null);
        JPanel root = new Theme.SmoothPanel(new BorderLayout());
        root.setBackground(new Color(233,233,233));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setContentPane(root);
        JPanel introduction = new Theme.RoundedPanel(new Color(246,246,246));
        introduction.setLayout(new BorderLayout(0,20));
        introduction.setPreferredSize(new Dimension(325, 420));
        introduction.setBorder(BorderFactory.createEmptyBorder(30, 26, 24, 26));
        JPanel words = Theme.clear(new GridLayout(3, 1, 0, 8));
        words.add(Theme.label("StackUp", 30, Theme.BLACK));
        words.add(Theme.label("Financial Management System", 13, Theme.MUTED));
        words.add(Theme.label("Clarity in every transaction.", 14, Theme.TEAL));
        introduction.add(words, BorderLayout.NORTH);
        introduction.add(new LoginIllustration());
        root.add(introduction, BorderLayout.WEST);

        boolean first = !service.hasAccounts();
        JPanel form = new Theme.RoundedPanel(Color.WHITE);
        form.setLayout(new BorderLayout(0,25));
        form.setBorder(BorderFactory.createEmptyBorder(48, 34, 44, 20));
        JPanel heading = Theme.clear(new GridLayout(2, 1, 0, 10));
        heading.add(Theme.label(first ? "Let's get started" : "Welcome back", 26, Theme.INK));
        JLabel description = new JLabel(first ? "Create your administrator account." : "Sign in to your StackUp account.");
        description.setForeground(Theme.MUTED);
        heading.add(description);
        form.add(heading, BorderLayout.NORTH);
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel fields = Theme.clear(new GridLayout(2, 1, 0, 22));
        fields.add(Theme.field("Username", username));
        fields.add(Theme.field("Password", password));
        form.add(fields);
        JButton login = Theme.button(first ? "Create Account" : "Log In");
        login.setPreferredSize(new Dimension(320, 44));
        form.add(login, BorderLayout.SOUTH);
        root.add(form);
        login.addActionListener(event -> {
            char[] value = password.getPassword();
            try {
                if (first) service.createAccount(username.getText(), value, "Admin");
                service.login(username.getText(), value);
                new DashboardUI(service).setVisible(true);
                dispose();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage());
            } finally {
                Arrays.fill(value, '\0');
                password.setText("");
            }
        });
        getRootPane().setDefaultButton(login);
    }

    /** Decorative ledger illustration, without sample transactions or amounts. */
    private static final class LoginIllustration extends JPanel {
        private static final long serialVersionUID = 1L;
        LoginIllustration() { setOpaque(false); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = getWidth() / 2 - 88, y = getHeight() / 2 - 68;
            pen.setColor(new Color(255, 255, 255, 28));
            pen.fillRoundRect(x + 18, y - 16, 174, 136, 20, 20);
            pen.setColor(new Color(235, 255, 249));
            pen.fillRoundRect(x, y, 174, 136, 20, 20);
            pen.setColor(new Color(177, 226, 212));
            pen.fillRoundRect(x + 19, y + 20, 74, 9, 7, 7);
            for (int line = 0; line < 3; line++) {
                pen.setColor(new Color(219, 242, 234));
                pen.fillRoundRect(x + 18, y + 47 + line * 23, 138, 13, 6, 6);
                pen.setColor(Theme.TEAL);
                pen.fillOval(x + 23, y + 50 + line * 23, 7, 7);
            }
            pen.setColor(new Color(132, 226, 190));
            pen.fillOval(x + 133, y + 90, 57, 57);
            pen.setColor(new Color(0, 99, 87));
            pen.setStroke(new BasicStroke(2));
            pen.drawLine(x + 150, y + 117, x + 159, y + 126);
            pen.drawLine(x + 159, y + 126, x + 174, y + 107);
            pen.dispose();
        }
    }
}

```

## src/frontend/PageSupport.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Shared display helpers; business rules remain in the backend. */
public abstract class PageSupport extends JPanel {
    private static final long serialVersionUID=1L;
    protected final DashboardUI window;
    protected final FinancialService service;
    protected final boolean admin;
    protected PageSupport(DashboardUI window) {
        super(new BorderLayout(12,12)); setOpaque(false);
        this.window=window; service=window.service();
        admin=service.getCurrent().getRole().equals("Admin");
    }
    protected JPanel emptyState(String title, String subtitle, int icon) {
        JPanel panel = Theme.clear(new GridBagLayout());
        JPanel stack = Theme.clear(new GridLayout(3, 1, 0, 8));
        JLabel badge = new JLabel(Theme.icon(icon, Theme.TEAL, true), SwingConstants.CENTER);
        stack.add(badge);
        JLabel heading = Theme.label(title, 14, Theme.INK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(heading);
        JLabel detail = new JLabel(subtitle, SwingConstants.CENTER);
        detail.setFont(new Font("Nimbus Sans", Font.PLAIN, 11));
        detail.setForeground(Theme.MUTED);
        stack.add(detail);
        panel.add(stack);
        return panel;
    }

    protected DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns,0) {
            private static final long serialVersionUID=1L;
            public boolean isCellEditable(int row,int column) { return false; }
        };
    }
    protected JTable table(DefaultTableModel model) {
        JTable table=new JTable(model); table.setRowHeight(30); table.setFont(new Font("DejaVu Sans",Font.PLAIN,11));
        table.setShowVerticalLines(false); table.setGridColor(Theme.LINE);
        table.setSelectionBackground(new Color(220,246,240));
        table.setSelectionForeground(Theme.INK); table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(240,242,245));
        table.getTableHeader().setFont(new Font("Nimbus Sans",Font.BOLD,10));
        table.getTableHeader().setPreferredSize(new Dimension(300, 28));
        javax.swing.table.DefaultTableCellRenderer header = new javax.swing.table.DefaultTableCellRenderer();
        header.setBackground(new Color(240,242,245));
        header.setForeground(Theme.INK);
        header.setFont(new Font("Nimbus Sans", Font.BOLD, 10));
        header.setBorder(BorderFactory.createEmptyBorder(7,8,7,8));
        table.getTableHeader().setDefaultRenderer(header);
        return table;
    }
    protected JPanel tableArea(JTable table,String message) {
        JPanel area=Theme.clear(new BorderLayout());
        JScrollPane scroll=new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        if(table.getRowCount()==0) {
            JPanel viewport=new JPanel(new BorderLayout()); viewport.setBackground(Color.WHITE);
            JLabel empty=new JLabel(message,SwingConstants.CENTER) {
                private static final long serialVersionUID = 1L;
                protected void paintComponent(Graphics graphics) {
                    Graphics2D pen = (Graphics2D) graphics.create();
                    pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    pen.setColor(new Color(242, 250, 246));
                    int diameter = Math.min(130, getHeight() - 20);
                    pen.fillOval((getWidth() - diameter) / 2, (getHeight() - diameter) / 2 - 10, diameter, diameter);
                    pen.dispose();
                    super.paintComponent(graphics);
                }
            };
            empty.setIcon(Theme.icon(1, Theme.TEAL, true));
            empty.setHorizontalTextPosition(SwingConstants.CENTER);
            empty.setVerticalTextPosition(SwingConstants.BOTTOM);
            empty.setIconTextGap(12);
            empty.setFont(new Font("Nimbus Sans",Font.PLAIN,12));
            empty.setForeground(Theme.MUTED); viewport.add(empty);
            table.putClientProperty("emptyLabel",empty);
            // JTable itself stays in the hierarchy for selection, refresh and tests.
            table.setLayout(new BorderLayout()); table.add(empty);
            table.getModel().addTableModelListener(event -> empty.setVisible(table.getRowCount()==0));
        }
        area.add(scroll); return area;
    }
    protected JPanel titled(String title,JComponent body) {
        JPanel card=Theme.card(); card.setLayout(new BorderLayout(0,10));
        card.add(Theme.label(title,15,Theme.INK),BorderLayout.NORTH); card.add(body); return card;
    }
    protected JPanel summary(LocalDate from,LocalDate to,boolean vertical) {
        JPanel row=Theme.clear(new GridLayout(vertical?3:1,vertical?1:3,10,10));
        BigDecimal revenue=service.total("Revenue",from,to),expense=service.total("Expense",from,to);
        BigDecimal[] values={revenue,expense,revenue.subtract(expense)};
        String[] names={"Revenue","Expenses","Profit"};
        Color[] accents={Theme.GREEN,Theme.RED,Theme.BLUE};
        Color[] fills={new Color(248,255,251),new Color(255,249,250),new Color(249,251,255)};
        for(int i=0;i<3;i++) {
            JPanel card=new Theme.RoundedPanel(fills[i]);
            card.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            card.setLayout(new BorderLayout(10,0));
            JLabel icon=new JLabel(Theme.icon(i==2?2:1,accents[i],true));
            card.add(icon,BorderLayout.WEST);
            JPanel text=Theme.clear(new GridLayout(2,1,0,3));
            text.add(new JLabel(names[i])); text.add(Theme.label(Theme.money(values[i]),24,accents[i]));
            card.add(text); row.add(card);
        }
        row.setPreferredSize(new Dimension(vertical?190:600,vertical?230:78)); return row;
    }
    protected void error(Exception exception) { JOptionPane.showMessageDialog(this,exception.getMessage(),"Check input",JOptionPane.ERROR_MESSAGE); }
    protected void record(String type,FinancialRecord existing) {
        if(RecordDialog.show(this,service,type,existing)) window.refresh();
    }
    protected void schedule() {
        if(PaymentDialog.show(this,service)) window.refresh();
    }
    protected void addRows(DefaultTableModel model,List<FinancialRecord> records) {
        for(FinancialRecord record:records) model.addRow(new Object[]{record.getDescription(),record.getCategory(),Theme.money(record.getAmount()),record.getDate().toLocalDate()});
    }
}

```

## src/frontend/PaymentDialog.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;

public final class PaymentDialog {
    private PaymentDialog() {}
    public static boolean show(Component owner,FinancialService service) {
        JTextField description=new JTextField(),category=new JTextField(),amount=new JTextField();
        DatePicker date=new DatePicker();
        JPanel form=Theme.clear(new GridLayout(4,1,0,12));
        form.add(Theme.field("Description",description)); form.add(Theme.field("Category",category));
        form.add(Theme.field("Amount",amount)); form.add(Theme.field("Due date",date));
        form.setPreferredSize(new Dimension(390,290));
        while(JOptionPane.showConfirmDialog(owner,form,"Schedule Payment",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                service.schedule(new Payment(description.getText(),category.getText(),new BigDecimal(amount.getText()),date.getSelectedDate()));
                return true;
            } catch(Exception exception) { JOptionPane.showMessageDialog(owner,exception.getMessage()); }
        }
        return false;
    }
}

```

## src/frontend/PaymentsUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class PaymentsUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public PaymentsUI(DashboardUI window) {
        super(window);
        JButton upcoming=Theme.button("Upcoming Payments"),paid=Theme.button("Paid Payments",Color.WHITE,Theme.INK),schedule=Theme.button("Schedule Payment");
        JPanel top=Theme.clear(new BorderLayout());JPanel tabs=Theme.clear(new FlowLayout(FlowLayout.LEFT,6,0));tabs.add(upcoming);tabs.add(paid);
        top.add(tabs,BorderLayout.WEST);top.add(schedule,BorderLayout.EAST);add(top,BorderLayout.NORTH);
        DefaultTableModel rows=model("Description","Category","Amount","Due Date","Status");JTable table=table(rows);add(tableArea(table,"No payments in this list yet."));
        java.util.List<Payment> visible=new ArrayList<>();boolean[] showPaid={false};
        Runnable refresh=()->{
            visible.clear();rows.setRowCount(0);
            for(Payment payment:service.getPayments())if(payment.isPaid()==showPaid[0]) {
                visible.add(payment);rows.addRow(new Object[]{payment.getDescription(),payment.getCategory(),Theme.money(payment.getAmount()),payment.getDue(),payment.isPaid()?"Paid":"Upcoming"});
            }
        };
        upcoming.addActionListener(event->{showPaid[0]=false;upcoming.setBackground(Theme.TEAL);upcoming.setForeground(Color.WHITE);paid.setBackground(Color.WHITE);paid.setForeground(Theme.INK);refresh.run();});
        paid.addActionListener(event->{showPaid[0]=true;paid.setBackground(Theme.TEAL);paid.setForeground(Color.WHITE);upcoming.setBackground(Color.WHITE);upcoming.setForeground(Theme.INK);refresh.run();});
        schedule.addActionListener(event->schedule());
        JButton pay=Theme.button("Mark Selected Paid");pay.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(event->pay.setEnabled(table.getSelectedRow()>=0&&!showPaid[0]));
        pay.addActionListener(event->{int row=table.getSelectedRow();if(row>=0&&JOptionPane.showConfirmDialog(this,"Record this payment as an expense?","Confirm Payment",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
            try{service.pay(visible.get(row).getId());refresh.run();}catch(Exception exception){error(exception);}
        }});
        JPanel actions=Theme.clear(new FlowLayout(FlowLayout.RIGHT,0,0));actions.add(pay);add(actions,BorderLayout.SOUTH);refresh.run();
    }
}

```

## src/frontend/RecordDialog.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import javax.swing.*;

public final class RecordDialog {
    private RecordDialog() {}
    public static boolean show(Component owner,FinancialService service,String type,FinancialRecord existing) {
        JTextField description=new JTextField(existing==null?"":existing.getDescription());
        JTextField category=new JTextField(existing==null?"":existing.getCategory());
        JTextField amount=new JTextField(existing==null?"":existing.getAmount().toPlainString());
        DatePicker date=new DatePicker();
        if(existing!=null) date.setSelectedDate(existing.getDate().toLocalDate());
        JPanel form=Theme.clear(new GridLayout(4,1,0,12));
        form.add(Theme.field("Description",description)); form.add(Theme.field("Category",category));
        form.add(Theme.field("Amount",amount)); form.add(Theme.field("Date",date));
        form.setPreferredSize(new Dimension(390,290));
        while(JOptionPane.showConfirmDialog(owner,form,"Record Transaction",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                LocalDateTime time=date.getSelectedDate().atTime(existing==null?LocalTime.now().withNano(0):existing.getDate().toLocalTime());
                BigDecimal value=new BigDecimal(amount.getText().trim());
                String author=existing==null?service.getCurrent().getUsername():existing.getRecordedBy();
                FinancialRecord record=type.equals("Revenue")?new Revenue(description.getText(),category.getText(),value,time,author):new Expense(description.getText(),category.getText(),value,time,author);
                if(existing==null) service.addRecord(record); else service.editRecord(existing.getId(),record);
                return true;
            } catch(Exception exception) { JOptionPane.showMessageDialog(owner,exception.getMessage()); }
        }
        return false;
    }
}

```

## src/frontend/Theme.java

```java
package frontend;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** The new mockup's design system; independent of the previous frontend. */
public final class Theme {
    public static final Color TEAL = new Color(0, 151, 139);
    public static final Color BLACK = new Color(23, 29, 29);
    public static final Color BACKGROUND = new Color(246, 246, 246);
    public static final Color INK = new Color(16, 30, 46);
    public static final Color MUTED = new Color(112, 123, 139);
    public static final Color LINE = new Color(227, 232, 237);
    public static final Color GREEN = new Color(0, 166, 92);
    public static final Color RED = new Color(244, 53, 81);
    public static final Color BLUE = new Color(31, 103, 234);

    private Theme() {}

    public static void install() {
        for (String name : new String[]{"NimbusSans-Regular.otf", "NimbusSans-Bold.otf", "DejaVuSans.ttf", "DejaVuSans-Bold.ttf"}) {
            try { GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
                Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets", name))); }
            catch (Exception ignored) { }
        }
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }
        Font font = new Font("Nimbus Sans", Font.PLAIN, 12);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font",
                "PasswordField.font", "Table.font", "TableHeader.font", "ComboBox.font"})
            UIManager.put(key, font);
        UIManager.put("ComboBoxUI", "frontend.ComboBoxSkin");
        UIManager.put("ComboBox.border", new InputBorder());
        UIManager.put("TextField.border", new InputBorder());
        UIManager.put("PasswordField.border", new InputBorder());
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", INK);
        UIManager.put("OptionPane.background", Color.WHITE);
    }

    public static String money(BigDecimal value) {
        return "₱" + String.format(Locale.US, "%,.2f", value);
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(text.contains("₱") ? "DejaVu Sans" : "Nimbus Sans", Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    public static Icon icon(int kind, Color color, boolean badge) {
        return new Icon() {
            public int getIconWidth() { return badge ? 38 : 18; }
            public int getIconHeight() { return badge ? 38 : 18; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (badge) {
                    pen.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),25));
                    pen.fillOval(x,y,38,38); x+=10; y+=10;
                }
                pen.setColor(color); pen.setStroke(new BasicStroke(1.3f));
                if (kind == 0) {
                    pen.drawLine(x+2,y+8,x+9,y+2); pen.drawLine(x+9,y+2,x+16,y+8);
                    pen.drawRect(x+4,y+8,10,8);
                } else if (kind == 2) {
                    pen.drawRect(x+3,y+10,3,6); pen.drawRect(x+8,y+6,3,10); pen.drawRect(x+13,y+2,3,14);
                } else if (kind == 3) {
                    pen.drawOval(x+2,y+2,14,14); pen.drawLine(x+9,y+5,x+9,y+13);pen.drawLine(x+5,y+9,x+13,y+9);
                } else if (kind == 4) {
                    pen.drawOval(x+2,y+2,14,14); pen.drawLine(x+9,y+4,x+9,y+9);pen.drawLine(x+9,y+9,x+13,y+10);
                } else {
                    pen.drawRoundRect(x+3,y+2,12,14,2,2);
                    pen.drawLine(x+6,y+6,x+12,y+6);pen.drawLine(x+6,y+10,x+12,y+10);
                }
                pen.dispose();
            }
        };
    }

    public static JPanel clear(LayoutManager layout) {
        JPanel panel = new SmoothPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel card() {
        JPanel panel = new RoundedPanel(Color.WHITE);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JButton button(String text) { return button(text, TEAL, Color.WHITE); }

    public static JButton button(String text, Color fill, Color ink) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = isEnabled() ? getBackground() : LINE;
                if (getModel().isPressed() || getModel().isRollover()) color = color.darker();
                pen.setColor(color);
                pen.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13);
                pen.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setBackground(fill);
        button.setForeground(ink);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setFont(new Font("Nimbus Sans", Font.BOLD, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JComponent field(String title, JComponent input) {
        JPanel panel = clear(new BorderLayout(0, 6));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(190, 34));
        input.setBorder(new InputBorder());
        panel.add(input);
        return panel;
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

    public static class InputBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        public Insets getBorderInsets(Component component) { return new Insets(6,8,6,8); }
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(6,8,6,8); return insets;
        }
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(LINE); pen.drawRoundRect(x,y,width-1,height-1,9,9); pen.dispose();
        }
    }

    public static class RoundedPanel extends SmoothPanel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        public RoundedPanel(Color fill) { super(new FlowLayout()); this.fill = fill; setOpaque(false); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(fill);
            pen.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            pen.setColor(fill.equals(BLACK) ? BLACK : LINE);
            pen.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
            pen.dispose();
        }
    }
}

```

## src/frontend/TransactionHistoryUI.java

```java
package frontend;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class TransactionHistoryUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public TransactionHistoryUI(DashboardUI window) {
        super(window);
        JComboBox<String> type=new JComboBox<>(new String[]{"All Transactions","Revenue","Expense"});
        JTextField search=new JTextField(); DatePicker from=new DatePicker(),to=new DatePicker();
        JButton apply=Theme.button("Apply"),reset=Theme.button("Reset",Color.WHITE,Theme.INK);
        JPanel controls=Theme.card();
        controls.setLayout(new BorderLayout(0,10));
        JPanel first=Theme.clear(new BorderLayout(10,0)); type.setPreferredSize(new Dimension(190,34));
        first.add(type,BorderLayout.WEST); first.add(search);
        JPanel second=Theme.clear(new GridLayout(1,4,10,0));
        second.add(Theme.field("From date",from)); second.add(Theme.field("To date",to));
        second.add(Theme.field(" ",apply)); second.add(Theme.field(" ",reset));
        controls.add(first,BorderLayout.NORTH); controls.add(second);
        add(controls,BorderLayout.NORTH);
        DefaultTableModel rows=model("Description","Category","Amount","Date","Recorded By");
        JTable table=table(rows); add(tableArea(table,"No transactions found."));
        java.util.List<FinancialRecord> visible=new ArrayList<>();
        Runnable refresh=()->{
            try {
                LocalDate start=from.getText().isBlank()?LocalDate.MIN:from.getSelectedDate();
                LocalDate end=to.getText().isBlank()?LocalDate.MAX:to.getSelectedDate();
                if(start.isAfter(end)) throw new IllegalArgumentException("From date must be before To date.");
                visible.clear(); rows.setRowCount(0);
                for(FinancialRecord record:service.getRecords()) if((type.getSelectedIndex()==0||record.getType().equals(type.getSelectedItem()))
                    && (record.getDescription()+" "+record.getCategory()).toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT))
                    && !record.getDate().toLocalDate().isBefore(start)&&!record.getDate().toLocalDate().isAfter(end)) {
                    visible.add(record); rows.addRow(new Object[]{record.getDescription(),record.getCategory(),Theme.money(record.getAmount()),record.getDate().toLocalDate(),record.getRecordedBy()});
                }
            } catch(Exception exception) { error(exception); }
        };
        apply.addActionListener(event->refresh.run()); search.addActionListener(event->refresh.run());
        type.addActionListener(event->refresh.run()); reset.addActionListener(event->{search.setText("");from.setSelectedDate(null);to.setSelectedDate(null);refresh.run();});
        if(admin) {
            JButton edit=Theme.button("Edit Selected"),delete=Theme.button("Delete Selected"); edit.setEnabled(false);delete.setEnabled(false);
            JPanel actions=Theme.clear(new FlowLayout(FlowLayout.RIGHT,6,0));actions.add(edit);actions.add(delete);controls.add(actions,BorderLayout.SOUTH);
            table.getSelectionModel().addListSelectionListener(event->{edit.setEnabled(table.getSelectedRow()>=0);delete.setEnabled(table.getSelectedRow()>=0);});
            edit.addActionListener(event->{int row=table.getSelectedRow();if(row>=0) record(visible.get(row).getType(),visible.get(row));});
            delete.addActionListener(event->{int row=table.getSelectedRow();if(row>=0&&JOptionPane.showConfirmDialog(this,"Delete this transaction?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                try {service.deleteRecord(visible.get(row).getId());refresh.run();}catch(Exception exception){error(exception);}
            }});
        }
        refresh.run();
    }
}

```