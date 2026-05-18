package biblioteca.gorbits.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Cuotas iguales espaciadas por {@code stepMonths} meses (por defecto 1) a partir de {@code firstDueDate}.
 */
public record EqualInstallmentPlanRequest(
        @NotNull @Positive @Max(120) Integer installmentCount,
        @NotNull LocalDate firstDueDate,
        @Positive @Max(24) Integer stepMonths
) {
}
