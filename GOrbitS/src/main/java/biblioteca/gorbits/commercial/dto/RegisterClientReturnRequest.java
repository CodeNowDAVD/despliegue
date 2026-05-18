package biblioteca.gorbits.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record RegisterClientReturnRequest(
        @NotBlank String reason,
        Instant returnedAt,
        boolean restoreStockToField,
        boolean hideFromReturnList) {}
