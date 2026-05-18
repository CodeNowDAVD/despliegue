package biblioteca.gorbits.billing.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RescheduleInstallmentRequest(@NotNull LocalDate dueDate) {
}
