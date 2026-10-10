package backend;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
/**
 * ABSTRACTION: the idea of "a money record". It cannot be created on its own, only as a
 * Revenue or an Expense. ENCAPSULATION: the fields are private and read through getters.
 */
public abstract class FinancialRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id = UUID.randomUUID().toString();

    private final String description, category, recordedBy;

    private final BigDecimal amount;

    private final LocalDateTime date;

    protected FinancialRecord(String description, String category, BigDecimal amount,
        LocalDateTime date, String recordedBy) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Description is required.");
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Category is required.");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException(
                "Enter a positive amount with up to two decimal places.");
        this.description = description.trim();
        this.category = category.trim();
        this.amount = amount;
        this.date = java.util.Objects.requireNonNull(date);
        this.recordedBy = recordedBy;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    /** Each subclass says what it is (polymorphism). */
    public abstract String getType();

    /** The amount as it affects profit: positive for revenue, negative for an expense. */
    public abstract BigDecimal signedAmount();
}
