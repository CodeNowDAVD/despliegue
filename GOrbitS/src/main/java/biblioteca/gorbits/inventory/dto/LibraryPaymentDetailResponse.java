package biblioteca.gorbits.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LibraryPaymentDetailResponse(
        Long id,
        BigDecimal amount,
        LocalDate paidOn,
        String note,
        Long campaignId,
        String campaignName,
        Instant createdAt) {}
