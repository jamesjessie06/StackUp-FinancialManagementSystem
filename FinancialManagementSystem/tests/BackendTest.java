import backend.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
public class BackendTest {
    private static void check(boolean ok, String name) {
        if (!ok)
            throw new AssertionError(name);
        System.out.println("PASS: " + name);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("fms-test-");
        System.setProperty("fms.data", dir.resolve("test.dat").toString());
        FinancialService s = FinancialService.getInstance();
        check(!s.hasAccounts(), "No default accounts");
        s.createAccount("owner", "password123".toCharArray(), "Admin");
        s.login("owner", "password123".toCharArray());
        check(s.getRecords().isEmpty() && s.getPayments().isEmpty(), "Empty first start");
        s.createAccount("staff", "password456".toCharArray(), "Employee");
        try {
            s.addRecord(
                new Revenue("test", "Sales", new BigDecimal("1"), LocalDateTime.now(), "owner"));
            throw new AssertionError("Admin recorded");
        } catch (SecurityException expected) {
            check(true, "Admin cannot record");
        }
        s.logout();
        s.login("staff", "password456".toCharArray());
        Revenue r = new Revenue(
            "Design", "Services", new BigDecimal("500.00"), LocalDateTime.now(), "staff");
        s.addRecord(r);
        Payment p = new Payment("Internet", "Utilities", new BigDecimal("100.00"), LocalDate.now());
        s.schedule(p);
        check(s.total("Expense", LocalDate.MIN, LocalDate.MAX).signum() == 0,
            "Scheduled payment is not an expense");
        s.pay(p.getId());
        check(s.total("Revenue", LocalDate.MIN, LocalDate.MAX).compareTo(new BigDecimal("500")) == 0
                && s.total("Expense", LocalDate.MIN, LocalDate.MAX).compareTo(new BigDecimal("100"))
                    == 0,
            "Payment updates financial totals");
        try {
            s.pay(s.getPayments().get(0).getId());
            throw new AssertionError("Duplicate pay");
        } catch (IllegalArgumentException expected) {
            check(true, "Duplicate payment blocked");
        }
        try {
            s.deleteRecord(r.getId());
            throw new AssertionError("Employee delete");
        } catch (SecurityException expected) {
            check(true, "Employee cannot delete");
        }
        s.logout();
        s.login("owner", "password123".toCharArray());
        s.editRecord(r.getId(),
            new Revenue(
                "Design revised", "Services", new BigDecimal("700.00"), r.getDate(), "staff"));
        check(
            s.total("Revenue", LocalDate.MIN, LocalDate.MAX).compareTo(new BigDecimal("700")) == 0,
            "Edits update totals");
        s.deleteRecord(s.getRecords().get(0).getId());
        check(s.total("Revenue", LocalDate.MIN, LocalDate.MAX).signum() == 0,
            "Deletes update totals");
        check(!s.getActivity().isEmpty(), "Activity log saved");
        java.lang.reflect.Field field = FinancialService.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
        s = FinancialService.getInstance();
        s.login("owner", "password123".toCharArray());
        check(s.getRecords().size() == 1 && s.getPayments().get(0).isPaid(),
            "Records and login survive restart");
        Files.delete(dir.resolve("test.dat"));
        Files.delete(dir);
    }
}
