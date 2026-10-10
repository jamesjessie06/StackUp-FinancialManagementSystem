package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class AccountsUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    public AccountsUI(DashboardUI window) {
        super(window);
        JPanel left = Theme.clear(new BorderLayout(0, 12));
        left.add(pendingCard(), BorderLayout.CENTER);

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


    @Override protected String subtitle() { return "Review new account requests, choose their role and reset employee passwords."; }

    private static JPasswordField passwordField(String hint) {
        Theme.RoundedPassword field = new Theme.RoundedPassword();
        field.setPlaceholder(hint);
        return field;
    }

    private JPanel pendingCard() {
        DefaultTableModel rows = model("Username", "Status");
        JTable requests = table(rows, "user", "muted");
        widths(requests, 160, 140);
        requests.setRowHeight(44);
        for (Account account : service.getPendingAccounts())
            rows.addRow(new Object[]{account.getUsername(), "Waiting for review"});
        JButton approve = Theme.button("Approve");
        JButton reject = Theme.outline("Reject");
        approve.setPreferredSize(new Dimension(100, 42));
        reject.setPreferredSize(new Dimension(100, 42));
        approve.setEnabled(false);
        reject.setEnabled(false);
        requests.getSelectionModel().addListSelectionListener(event -> {
            boolean chosen = requests.getSelectedRow() >= 0;
            approve.setEnabled(chosen);
            reject.setEnabled(chosen);
        });
        approve.addActionListener(event -> review(requests, rows, true));
        reject.addActionListener(event -> review(requests, rows, false));

        JPanel buttons = Theme.clear(new GridLayout(1, 2, 10, 0));
        buttons.add(approve);
        buttons.add(reject);
        JPanel title = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        title.add(Theme.label("Pending Requests", 22, Theme.INK));
        title.add(Theme.muted("· " + rows.getRowCount(), 15));
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(title, BorderLayout.NORTH);
        card.add(tableArea(requests, "No accounts waiting for approval."));
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private void review(JTable requests, DefaultTableModel rows, boolean approve) {
        int row = requests.getSelectedRow();
        if (row < 0) return;
        String name = (String) rows.getValueAt(row, 0);
        String[] chosenRole = {"Employee"};
        boolean yes;
        if (approve) {
            JPanel roles = Theme.segmented(new String[]{"Employee", "Admin"}, 0, Theme.INK, index -> chosenRole[0] = index == 0 ? "Employee" : "Admin");
            JPanel stretch = Theme.clear(new GridLayout(1, 1));
            stretch.add(roles);
            JPanel body = Theme.clear(new GridLayout(1, 1));
            body.add(Theme.field("Role", stretch));
            body.setPreferredSize(new Dimension(372, 80));
            yes = Theme.form(this, Icons.Kind.CHECK, "Approve account", "Choose what " + name + " can do after signing in.", body, "Approve", "Cancel");
        } else
            yes = Theme.confirm(this, Icons.Kind.CLOSE, "Reject request?", "The request from " + name + " will be removed.", "Yes, reject", "Cancel");
        if (!yes) return;
        try {
            if (approve) service.approveAccount(name, chosenRole[0]); else service.rejectAccount(name);
            window.refresh();
        } catch (Exception exception) {
            error(exception);
        }
    }

}
