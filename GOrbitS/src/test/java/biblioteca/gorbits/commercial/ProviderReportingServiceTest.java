package biblioteca.gorbits.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.billing.dto.ProviderBillingTotalsResponse;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.LibraryReconciliationSummaryResponse;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderReportingServiceTest {

    @Mock
    CampaignRepository campaigns;

    @Mock
    SalesGuideRepository guides;

    @Mock
    BillingService billing;

    @Mock
    InventoryService inventory;

    @InjectMocks
    ProviderReportingService service;

    private final UserAccount owner = UnitTestFixtures.proveedor(1L);

    @Test
    void campaignCloseReport_agregaTotales() {
        var campaign = UnitTestFixtures.campaign(5L, "Verano");
        when(campaigns.findById(5L)).thenReturn(Optional.of(campaign));
        when(guides.countByOwner_IdAndCampaign_IdAndStatus(1L, 5L, GuideStatus.ACTIVA)).thenReturn(2L);
        when(guides.countByOwner_IdAndCampaign_IdAndStatus(1L, 5L, GuideStatus.CERRADA)).thenReturn(3L);
        when(guides.countByOwner_IdAndCampaign_IdAndStatus(1L, 5L, GuideStatus.DEVUELTA)).thenReturn(1L);
        when(guides.sumLineQtyForOwnerCampaignAndStatus(1L, 5L, GuideStatus.CERRADA)).thenReturn(10L);
        when(guides.findByOwner_IdAndCampaign_IdAndStatus(1L, 5L, GuideStatus.CERRADA)).thenReturn(List.of());

        var billingTotals = new ProviderBillingTotalsResponse(5L, "Verano", bd("500"), bd("200"), bd("300"), 2, 1);
        when(billing.providerTotals(owner, 5L)).thenReturn(billingTotals);
        var library = new LibraryReconciliationSummaryResponse(
                5L, "Verano", campaign.getStartsOn(), campaign.getEndsOn(), 20, 2, 18, 10, bd("1000"), bd("800"), bd("200"));
        when(inventory.libraryReconciliationSummary(owner, 5L)).thenReturn(library);

        var report = service.campaignCloseReport(owner, 5L);

        assertThat(report.campaignName()).isEqualTo("Verano");
        assertThat(report.guidesActive()).isEqualTo(2L);
        assertThat(report.guidesClosed()).isEqualTo(3L);
        assertThat(report.totalToCollect()).isEqualByComparingTo("500");
        assertThat(report.librarySummary().netBalanceOwedToLibrary()).isEqualByComparingTo("200");
    }

    @Test
    void campaignCloseReport_campanaInexistente() {
        when(campaigns.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.campaignCloseReport(owner, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
