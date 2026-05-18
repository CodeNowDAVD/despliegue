package biblioteca.gorbits.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentCalendarItemResponse(
        Long installmentId,
        Long guideId,
        String contractNumber,
        String clientName,
        int sequence,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidTotal,
        BigDecimal remaining
) {
}
