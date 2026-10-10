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
        widths(table, 190, 135, 125, 110);
        card.add(tableArea(table, "You haven't recorded anything yet."));

        return card;
    }
}
