package biblioteca.gorbits.commercial.dto;

import biblioteca.gorbits.inventory.dto.LibraryReconciliationSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CampaignCloseReportResponse(
        Long campaignId,
        String campaignName,
        LocalDate startsOn,
        LocalDate endsOn,
        long guidesActive,
        long guidesClosed,
        long guidesReturned,
        long unitsSold,
        BigDecimal totalSoldAmount,
        BigDecimal totalToCollect,
        BigDecimal totalCollected,
        BigDecimal totalPending,
        LibraryReconciliationSummaryResponse librarySummary
) {
}
