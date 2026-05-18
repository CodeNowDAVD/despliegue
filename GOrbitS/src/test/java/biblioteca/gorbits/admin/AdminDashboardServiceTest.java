package biblioteca.gorbits.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceRepository;
import biblioteca.gorbits.inventory.ProviderFieldStockRepository;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    SalesGuideRepository salesGuides;

    @Mock
    WarehouseStockRepository warehouse;

    @Mock
    ProviderFieldStockRepository fieldStock;

    @Mock
    LibrarySupplyInvoiceRepository libraryInvoices;

    @InjectMocks
    AdminDashboardService service;

    @Test
    void dashboard_agregaContadoresYRanking() {
        when(salesGuides.countByStatus(GuideStatus.ACTIVA)).thenReturn(3L);
        when(salesGuides.countByStatus(GuideStatus.CERRADA)).thenReturn(7L);
        when(salesGuides.countByStatus(GuideStatus.DEVUELTA)).thenReturn(1L);
        when(warehouse.sumTotalQuantity()).thenReturn(100L);
        when(fieldStock.sumTotalQuantity()).thenReturn(40L);
        when(libraryInvoices.sumAllPurchasedUnits()).thenReturn(200L);
        when(salesGuides.sumUnitsByBookForGuideStatus(eq(GuideStatus.CERRADA), any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[] {1L, "Biblia", 15L}));

        var dash = service.dashboard();

        assertThat(dash.guidesByStatus().activa()).isEqualTo(3L);
        assertThat(dash.guidesByStatus().cerrada()).isEqualTo(7L);
        assertThat(dash.totalWarehouseUnits()).isEqualTo(100L);
        assertThat(dash.topBooksClosedGuides()).hasSize(1);
        assertThat(dash.topBooksClosedGuides().getFirst().title()).isEqualTo("Biblia");
    }
}
