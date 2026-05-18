package biblioteca.gorbits.admin;

import biblioteca.gorbits.admin.dto.AdminDashboardResponse;
import biblioteca.gorbits.admin.dto.BookSalesRankItem;
import biblioteca.gorbits.admin.dto.GuideStatusSummary;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceRepository;
import biblioteca.gorbits.inventory.ProviderFieldStockRepository;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private static final int TOP_BOOKS_LIMIT = 10;

    private final SalesGuideRepository salesGuides;
    private final WarehouseStockRepository warehouse;
    private final ProviderFieldStockRepository fieldStock;
    private final LibrarySupplyInvoiceRepository libraryInvoices;

    public AdminDashboardService(
            SalesGuideRepository salesGuides,
            WarehouseStockRepository warehouse,
            ProviderFieldStockRepository fieldStock,
            LibrarySupplyInvoiceRepository libraryInvoices) {
        this.salesGuides = salesGuides;
        this.warehouse = warehouse;
        this.fieldStock = fieldStock;
        this.libraryInvoices = libraryInvoices;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        GuideStatusSummary gs =
                new GuideStatusSummary(
                        salesGuides.countByStatus(GuideStatus.ACTIVA),
                        salesGuides.countByStatus(GuideStatus.CERRADA),
                        salesGuides.countByStatus(GuideStatus.DEVUELTA));
        long warehouseSum = warehouse.sumTotalQuantity();
        long fieldSum = fieldStock.sumTotalQuantity();
        long librarySum = libraryInvoices.sumAllPurchasedUnits();
        List<BookSalesRankItem> ranking = mapRanking(GuideStatus.CERRADA);
        return new AdminDashboardResponse(gs, warehouseSum, fieldSum, librarySum, ranking);
    }

    private List<BookSalesRankItem> mapRanking(GuideStatus status) {
        List<Object[]> rows =
                salesGuides.sumUnitsByBookForGuideStatus(status, PageRequest.of(0, TOP_BOOKS_LIMIT));
        List<BookSalesRankItem> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            long bookId = ((Number) row[0]).longValue();
            String title = (String) row[1];
            long units = ((Number) row[2]).longValue();
            out.add(new BookSalesRankItem(bookId, title, units));
        }
        return out;
    }
}
