package biblioteca.gorbits.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        Long id,
        Long guideId,
        int sequence,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidTotal,
        BigDecimal remaining,
        boolean fullyPaid
) {
}
