package backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/** A expense transaction inherits the common financial fields. */
public final class Expense extends FinancialRecord {

    private static final long serialVersionUID = 1L;

    public Expense(String description, String category, BigDecimal amount, LocalDateTime date,
        String recordedBy) {
        super(description, category, amount, date, recordedBy);
    }

    @Override

    public String getType() {
        return "Expense";
    }
}
