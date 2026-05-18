package biblioteca.gorbits.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        GuideStatusSummary guidesByStatus,
        long totalWarehouseUnits,
        long totalFieldStockUnits,
        long totalLibraryPurchasedUnits,
        List<BookSalesRankItem> topBooksClosedGuides) {}
