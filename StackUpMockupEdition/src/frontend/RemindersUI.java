package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Every unpaid payment in due-date order, so nothing is missed. */
public final class RemindersUI extends PageSupport {
    private static final long serialVersionUID = 1L;

    public RemindersUI(DashboardUI window) {
        super(window);
        List<Payment> pending = new ArrayList<>();
        for (Payment payment : service.getPayments()) if (!payment.isPaid()) pending.add(payment);
        pending.sort(Comparator.comparing(Payment::getDue));

        int overdue = 0, thisWeek = 0, later = 0;
        BigDecimal weekTotal = BigDecimal.ZERO;
        for (Payment payment : pending) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), payment.getDue());
            if (days < 0) overdue++;
            else if (days <= 7) { thisWeek++; weekTotal = weekTotal.add(payment.getAmount()); }
            else later++;
        }
        JPanel stats = Theme.equal(12,
            stat("Overdue", Integer.toString(overdue), overdue == 0 ? "Nothing is late" : "Needs attention", STYLE_DARK, Icons.Kind.BELL, Color.WHITE, 26),
            stat("Due This Week", Integer.toString(thisWeek), thisWeek == 0 ? "Nothing due" : Theme.whole(weekTotal) + " to pay", STYLE_GREEN, Icons.Kind.CLOCK, Color.WHITE, 26),
            stat("Coming Up Later", Integer.toString(later), "Scheduled after 7 days", STYLE_WHITE, Icons.Kind.CALENDAR, Theme.INK, 26));
        stats.setPreferredSize(new Dimension(700, 120));
        add(stats, BorderLayout.NORTH);

        DefaultTableModel rows = model("Reminder", "Category", "Amount", "Due Date", "Status");
        for (Payment payment : pending)
            rows.addRow(new Object[]{new Tile(payment.getDue(), payment.getDescription()), payment.getCategory(),
                Theme.money(payment.getAmount()), payment.getDue().format(DAY), dueText(payment)});
        JTable table = table(rows, "tile", "muted", "bold", "muted", "chip");
        table.setRowHeight(54);
        widths(table, 240, 110, 120, 120, 160);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Theme.label("Payment Reminders", 20, Theme.INK), BorderLayout.NORTH);
        card.add(tableArea(table, "No reminders"));
        add(card);
    }

    @Override protected String subtitle() { return "Every payment still to be made, soonest first."; }

    /** Employees can jump straight to the page where payments are marked paid. */
    @Override protected JComponent actions() {
        if (admin) return null;
        JButton open = Theme.dark("Open Payments");
        open.setIcon(Icons.of(Icons.Kind.CLOCK, Color.WHITE, 18));
        open.addActionListener(event -> window.navigate("Payments"));
        return open;
    }
}
