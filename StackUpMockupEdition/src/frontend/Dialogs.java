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
