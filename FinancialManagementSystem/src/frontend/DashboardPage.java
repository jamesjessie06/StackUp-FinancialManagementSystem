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

    /** The month label. */
    @Override protected JComponent actions() {
        JPanel row = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        JButton month = Theme.outline(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
        month.setIcon(Icons.of(Icons.Kind.CALENDAR, Theme.INK, 18));
        month.setFocusable(false);
        month.setCursor(Cursor.getDefaultCursor());
        row.add(month);
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
