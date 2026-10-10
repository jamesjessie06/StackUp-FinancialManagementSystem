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
