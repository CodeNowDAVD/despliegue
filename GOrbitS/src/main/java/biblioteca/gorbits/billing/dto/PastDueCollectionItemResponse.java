package biblioteca.gorbits.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cobranza con fecha de vencimiento pasada y saldo pendiente (lenguaje neutro). */
public record PastDueCollectionItemResponse(
        Long installmentId,
        Long guideId,
        String contractNumber,
        Long clientId,
        String clientName,
        int installmentSeq,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        long daysPastDue
) {
}
