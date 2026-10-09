package backend;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public final class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id, description, category;

    private final BigDecimal amount;

    private final LocalDate due;

    private final boolean paid;

    /** Who scheduled it; null for payments saved before this was tracked. */
    private final String scheduledBy;

    public Payment(String description, String category, BigDecimal amount, LocalDate due) {
        this(UUID.randomUUID().toString(), description, category, amount, due, false, null);
    }

    public Payment(String description, String category, BigDecimal amount, LocalDate due,
        String scheduledBy) {
        this(UUID.randomUUID().toString(), description, category, amount, due, false, scheduledBy);
    }

    public String getScheduledBy() {
        return scheduledBy;
    }

    private Payment(String id, String description, String category, BigDecimal amount,
        LocalDate due, boolean paid, String scheduledBy) {
        if (description.isBlank() || category.isBlank() || amount.signum() <= 0
            || amount.scale() > 2)
            throw new IllegalArgumentException(
                "Description, category and a positive amount are required.");
        this.description = description.trim();
        this.category = category.trim();
        this.amount = amount;
        this.due = java.util.Objects.requireNonNull(due);
        this.paid = paid;
        this.scheduledBy = scheduledBy;
        this.id = id;
    }

    public Payment asPaid() {
        return new Payment(id, description, category, amount, due, true, scheduledBy);
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

    public LocalDate getDue() {
        return due;
    }

    public boolean isPaid() {
        return paid;
    }
}
