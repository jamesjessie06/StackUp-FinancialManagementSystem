import backend.*;
import frontend.*;
import frontend.style.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Exercises actual Swing buttons with isolated temporary records. Requires a display. */
public class FrontendTest {
    private static DashboardUI window;
    private static FinancialService service;
    private static <T> List<T> find(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child))
                result.add(type.cast(child));
            if (child instanceof Container)
                result.addAll(find((Container) child, type));
        }
        return result;
    }
    private static void click(String text) {
        for (JButton button : find(window, JButton.class))
            if (text.equals(button.getText()) || text.equals(button.getName())) {
                if (!button.isEnabled())
                    throw new AssertionError("Disabled: " + text);
                button.doClick();
                return;
            }
        throw new AssertionError("Missing button: " + text);
    }
    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }
    private static void fillDialog(String[] values, int answer) {
        javax.swing.Timer timer = new javax.swing.Timer(200, event -> {
            for (Window dialog : Window.getWindows())
                if (dialog instanceof JDialog && dialog.isVisible()) {
                    List<JOptionPane> panes = find((Container) dialog, JOptionPane.class);
                    if (!panes.isEmpty()) {
                        List<JTextField> fields = find(panes.get(0), JTextField.class);
                        for (int i = 0; i < values.length; i++) {
                            if (fields.get(i) instanceof DatePicker)
                                ((DatePicker) fields.get(i)).setSelectedDate(LocalDate.parse(values[i].split(" ")[0]));
                            else fields.get(i).setText(values[i]);
                        }
                        panes.get(0).setValue(answer);
                        return;
                    }
                }
            throw new AssertionError("No dialog opened");
        });
        timer.setRepeats(false);
        timer.start();
    }
    private static void fillForm(String description, String amount) {
        List<JTextField> fields = find(window, JTextField.class);
        fields.get(0).setText(description);
        fields.get(1).setText(amount);
    }
    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("wealth-ui-test-").resolve("finance.dat");
        System.setProperty("fms.data", data.toString());
        service = FinancialService.getInstance();
        service.createAccount("owner", "password123".toCharArray(), "Admin");
        service.login("owner", "password123".toCharArray());
        service.createAccount("staff", "password456".toCharArray(), "Employee");
        service.logout();
        service.login("staff", "password456".toCharArray());
        SwingUtilities.invokeAndWait(() -> {
            try {
                Theme.install();
                window = new DashboardUI(service);
                window.setVisible(true);
                click("Financial Record");
                fillForm("Service", "500.00");
                click("Save Revenue");
                check(service.getRecords().size() == 1, "Revenue button saves entered data");
                click("Expense");
                fillForm("Supplies", "100.00");
                click("Save Expense");
                check(find(window, JTable.class).get(0).getRowCount() == 2,
                    "Recent records refresh after saving");
                fillDialog(new String[] {}, JOptionPane.OK_OPTION);
                click("Financial Record");
                check(service.getRecords().size() == 2, "Repeated menu opens a warning without duplicating records");
                click("Dashboard");
                fillDialog(new String[] {}, JOptionPane.CANCEL_OPTION);
                click("Add Revenue");
                check(service.getRecords().size() == 2, "Cancel closes the themed form without saving");
                click("Transaction History");
                check(find(window, JTable.class).get(0).getRowCount() == 2,
                    "All Transactions shows both revenue and expense");
                click("Revenue");
                check(find(window, JTable.class).get(0).getRowCount() == 1,
                    "Revenue filter displays saved record");
                click("Expenses");
                check(find(window, JTable.class).get(0).getRowCount() == 1,
                    "Expense tab displays saved record");
                check(find(window, JButton.class)
                          .stream()
                          .noneMatch(button -> button.getText().equals("Edit Selected")),
                    "Employee has no edit action");
                find(window, JTextField.class).get(0).setText("missing");
                click("Apply");
                check(
                    find(window, JTable.class).get(0).getRowCount() == 0, "Search filters records");
                click("Reset");
                click("Payments");
                fillDialog(new String[] {"Internet", "Bills", "50.00", LocalDate.now().toString()},
                    JOptionPane.OK_OPTION);
                click("Schedule Payment");
                check(find(window, JTable.class).get(0).getRowCount() == 1,
                    "Scheduling refreshes payment list");
                find(window, JTable.class).get(0).setRowSelectionInterval(0, 0);
                fillDialog(new String[] {}, JOptionPane.YES_OPTION);
                click("Mark Selected Paid");
                check(service.total("Expense", LocalDate.MIN, LocalDate.MAX)
                            .compareTo(new BigDecimal("150"))
                        == 0,
                    "Payment action records one expense");
                check(find(window, JTable.class).get(0).getRowCount() == 0,
                    "Paid item leaves Upcoming tab");
                window.dispose();
                service.logout();
                service.login("owner", "password123".toCharArray());
                window = new DashboardUI(service);
                window.setVisible(true);
                click("Financial Report");
                click("Generate Report");
                check(find(window, ChartPanel.class).size() == 1, "Report displays its chart");
                find(window, DatePicker.class).get(0).setSelectedDate(LocalDate.now().withDayOfMonth(1));
                find(window, DatePicker.class).get(1).setSelectedDate(LocalDate.now().withDayOfMonth(1)
                    .plusMonths(1).minusDays(1));
                click("Generate Report");
                check(find(window, ChartPanel.class).size() == 1, "Month report draws its chart");
                find(window, DatePicker.class).get(0).setSelectedDate(LocalDate.now().withDayOfYear(1));
                find(window, DatePicker.class).get(1).setSelectedDate(LocalDate.now().withDayOfYear(1)
                    .plusYears(1).minusDays(1));
                click("Generate Report");
                check(find(window, ChartPanel.class).size() == 1, "Year report draws its chart");
                click("Financial Insights");
                check(find(window, ChartPanel.class).size() == 3,
                    "Insights displays category charts and profit trend");
                click("Transaction History");
                check(!find(window, JButton.class)
                           .stream()
                           .filter(button -> button.getText().equals("Edit Selected"))
                           .findFirst()
                           .orElseThrow()
                           .isEnabled(),
                    "Edit disabled until selection");
                find(window, JTable.class).get(0).setRowSelectionInterval(0, 0);
                check(find(window, JButton.class)
                          .stream()
                          .filter(button -> button.getText().equals("Edit Selected"))
                          .findFirst()
                          .orElseThrow()
                          .isEnabled(),
                    "Admin edit enabled with selection");
                click("Activity Log");
                check(find(window, JTable.class).get(0).getRowCount() >= 4,
                    "Activity table displays recorded changes");
                window.dispose();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        Files.delete(data);
        Files.delete(data.getParent());
        System.exit(0);
    }
}
