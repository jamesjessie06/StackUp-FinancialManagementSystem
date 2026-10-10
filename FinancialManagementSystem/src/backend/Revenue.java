package backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class Revenue extends FinancialRecord {

    private static final long serialVersionUID = 1L;

    public Revenue(String description, String category, BigDecimal amount, LocalDateTime date,
        String recordedBy) {
        super(description, category, amount, date, recordedBy);
    }

    @Override

    public String getType() {
        return "Revenue";
    }

    @Override
    public BigDecimal signedAmount() {
        return getAmount();
    }
}
