import backend.*;
import java.nio.file.*;

/** Tests employee password recovery and admin-only access without real application data. */
public final class AccountManagementTest {
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempDirectory("account-test-").resolve("data.dat");
        System.setProperty("fms.data", file.toString());
        FinancialService service = FinancialService.getInstance();
        service.createAccount("owner", "password123".toCharArray(), "Admin");
        service.login("owner", "password123".toCharArray());
        service.createAccount("staff", "password456".toCharArray(), "Employee");
        if (service.getAccounts().size() != 2) throw new AssertionError("Missing accounts");
        service.resetEmployeePassword("staff", "newpassword789".toCharArray());
        service.logout();
        try { service.login("staff", "password456".toCharArray());
            throw new AssertionError("Old password accepted"); }
        catch (IllegalArgumentException expected) { }
        service.login("staff", "newpassword789".toCharArray());
        try { service.getAccounts(); throw new AssertionError("Employee can list accounts"); }
        catch (SecurityException expected) { }
        try { service.resetEmployeePassword("staff", "otherpass123".toCharArray());
            throw new AssertionError("Employee can reset password"); }
        catch (SecurityException expected) { }
        System.out.println("PASS: Account list, password reset, old password rejection and role protection");
        Files.delete(file); Files.delete(file.getParent());
    }
}
