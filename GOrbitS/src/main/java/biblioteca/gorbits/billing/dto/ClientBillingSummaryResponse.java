package biblioteca.gorbits.billing.dto;

import java.math.BigDecimal;

public record ClientBillingSummaryResponse(
        Long clientId,
        String clientName,
        Long campaignId,
        String campaignName,
        int guideCount,
        BigDecimal totalToCollect,
        BigDecimal totalCollected,
        BigDecimal totalPending,
        int pendingInstallmentCount
) {
}
