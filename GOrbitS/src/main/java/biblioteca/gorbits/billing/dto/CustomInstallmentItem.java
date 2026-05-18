package biblioteca.gorbits.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomInstallmentItem(
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount
) {
}
