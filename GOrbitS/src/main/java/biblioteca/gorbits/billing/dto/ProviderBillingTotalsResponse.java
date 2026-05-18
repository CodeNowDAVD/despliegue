package biblioteca.gorbits.billing.dto;

import java.math.BigDecimal;

public record ProviderBillingTotalsResponse(
        Long campaignId,
        String campaignName,
        BigDecimal totalToCollect,
        BigDecimal totalCollected,
        BigDecimal totalPending,
        int pendingInstallmentCount,
        int pastDueInstallmentCount
) {
}
