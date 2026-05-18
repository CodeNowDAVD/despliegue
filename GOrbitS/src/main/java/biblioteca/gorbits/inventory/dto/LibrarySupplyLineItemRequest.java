package biblioteca.gorbits.inventory.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Línea de factura de librería: el importe se calcula desde el precio del catálogo (× cantidad, − descuento opcional). */
public record LibrarySupplyLineItemRequest(
        @NotNull Long bookId,
        @Positive int quantity,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercent) {}
