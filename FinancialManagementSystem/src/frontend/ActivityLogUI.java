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
