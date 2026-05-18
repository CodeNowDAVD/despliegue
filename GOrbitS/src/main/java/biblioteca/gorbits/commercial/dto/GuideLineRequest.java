package biblioteca.gorbits.commercial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record GuideLineRequest(
        @NotNull @Positive Long bookId,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal unitPrice
) {
}
