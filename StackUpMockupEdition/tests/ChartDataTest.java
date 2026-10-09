import backend.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class ChartDataTest {
    private static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }
    public static void main(String[] args) {
        List<FinancialRecord> records =
            List.of(new Revenue("Service", "Services", new BigDecimal("100"),
                        LocalDateTime.of(2026, 1, 1, 10, 0), "staff"),
                new Expense("Rent", "Bills", new BigDecimal("200"),
                    LocalDateTime.of(2026, 1, 2, 10, 0), "staff"),
                new Expense("Internet", "Bills", new BigDecimal("50"),
                    LocalDateTime.of(2026, 2, 1, 10, 0), "staff"),
                new Revenue("Previous year", "Services", new BigDecimal("999"),
                    LocalDateTime.of(2025, 1, 1, 10, 0), "staff"));
        check(ChartData.categories(records, "Expense").get("Bills").compareTo(new BigDecimal("250"))
                == 0,
            "Expense category amounts combine correctly");
        BigDecimal[][] monthly = ChartData.monthly(records, 2026);
        check(monthly[0][0].compareTo(new BigDecimal("100")) == 0, "Trend excludes other years");
        check(monthly[2][0].compareTo(new BigDecimal("-100")) == 0,
            "Trend preserves negative profit");
        check(monthly[2][2].signum() == 0, "Months without records stay zero");
        check(ChartData.between(records, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)).size()
                == 2,
            "Date boundaries are inclusive");
        check(ChartData.categories(List.of(), "Revenue").isEmpty(),
            "Empty chart has no invented categories");
    }
}
