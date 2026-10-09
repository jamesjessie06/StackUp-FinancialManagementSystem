package backend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Converts transaction records into chart totals. Contains no Swing code. */
public final class ChartData {

    private ChartData() {}

    public static BigDecimal total(List<FinancialRecord> records, String type) {
        BigDecimal total = BigDecimal.ZERO;
        for (FinancialRecord record : records) {
            if (record.getType().equals(type))
                total = total.add(record.getAmount());
        }
        return total;
    }

    /** Profit of a list of records: revenues add, expenses subtract (each record decides its own sign). */
    public static BigDecimal net(List<FinancialRecord> records) {
        BigDecimal net = BigDecimal.ZERO;
        for (FinancialRecord record : records) net = net.add(record.signedAmount());
        return net;
    }

    public static Map<String, BigDecimal> categories(List<FinancialRecord> records, String type) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        for (FinancialRecord record : records) {
            if (record.getType().equals(type)) {
                String category = record.getCategory();
                totals.put(category,
                    totals.getOrDefault(category, BigDecimal.ZERO).add(record.getAmount()));
            }
        }
        return totals;
    }

    public static List<FinancialRecord> between(
        List<FinancialRecord> records, LocalDate from, LocalDate to) {
        List<FinancialRecord> matching = new ArrayList<>();
        for (FinancialRecord record : records) {
            LocalDate date = record.getDate().toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to))
                matching.add(record);
        }
        return matching;
    }

    public static BigDecimal[][] monthly(List<FinancialRecord> records, int year) {
        BigDecimal[][] amounts = new BigDecimal[3][12];
        for (int series = 0; series < 3; series++) Arrays.fill(amounts[series], BigDecimal.ZERO);
        for (FinancialRecord record : records) {
            if (record.getDate().getYear() == year) {
                int month = record.getDate().getMonthValue() - 1;
                int series = record.signedAmount().signum() > 0 ? 0 : 1;   // polymorphism: each record knows its sign
                amounts[series][month] = amounts[series][month].add(record.getAmount());
            }
        }
        for (int month = 0; month < 12; month++)
            amounts[2][month] = amounts[0][month].subtract(amounts[1][month]);
        return amounts;
    }
}
