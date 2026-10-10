package frontend;

import backend.*;
import frontend.style.*;
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
        widths(table, 185, 105, 130, 120, 110, 130);
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
