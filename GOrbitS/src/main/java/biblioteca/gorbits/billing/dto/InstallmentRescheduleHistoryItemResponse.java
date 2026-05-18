package biblioteca.gorbits.billing.dto;

import java.time.Instant;
import java.time.LocalDate;

public record InstallmentRescheduleHistoryItemResponse(
        Long id, LocalDate previousDueDate, LocalDate newDueDate, Instant rescheduledAt) {
}
