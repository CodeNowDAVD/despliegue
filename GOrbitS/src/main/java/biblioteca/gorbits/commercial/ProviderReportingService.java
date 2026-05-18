package biblioteca.gorbits.commercial;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.billing.dto.ProviderBillingTotalsResponse;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.dto.CampaignCloseReportResponse;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.LibraryReconciliationSummaryResponse;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderReportingService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final CampaignRepository campaigns;
    private final SalesGuideRepository guides;
    private final BillingService billing;
    private final InventoryService inventory;

    public ProviderReportingService(
            CampaignRepository campaigns,
            SalesGuideRepository guides,
            BillingService billing,
            InventoryService inventory) {
        this.campaigns = campaigns;
        this.guides = guides;
        this.billing = billing;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public CampaignCloseReportResponse campaignCloseReport(UserAccount owner, Long campaignId) {
        Campaign c = campaigns
                .findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaña no encontrada"));
        long ownerId = owner.getId();
        long active = guides.countByOwner_IdAndCampaign_IdAndStatus(ownerId, campaignId, GuideStatus.ACTIVA);
        long closed = guides.countByOwner_IdAndCampaign_IdAndStatus(ownerId, campaignId, GuideStatus.CERRADA);
        long returned = guides.countByOwner_IdAndCampaign_IdAndStatus(ownerId, campaignId, GuideStatus.DEVUELTA);
        long unitsSold = guides.sumLineQtyForOwnerCampaignAndStatus(ownerId, campaignId, GuideStatus.CERRADA);
        BigDecimal totalSold = guides.findByOwner_IdAndCampaign_IdAndStatus(ownerId, campaignId, GuideStatus.CERRADA)
                .stream()
                .map(this::guideLinesTotal)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        ProviderBillingTotalsResponse billingTotals = billing.providerTotals(owner, campaignId);
        LibraryReconciliationSummaryResponse library =
                inventory.libraryReconciliationSummary(owner, campaignId);
        return new CampaignCloseReportResponse(
                c.getId(),
                c.getName(),
                c.getStartsOn(),
                c.getEndsOn(),
                active,
                closed,
                returned,
                unitsSold,
                totalSold,
                billingTotals.totalToCollect(),
                billingTotals.totalCollected(),
                billingTotals.totalPending(),
                library);
    }

    private BigDecimal guideLinesTotal(SalesGuide guide) {
        BigDecimal t = ZERO;
        for (SalesGuideLine line : guide.getLines()) {
            t = t.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        return t.setScale(2, RoundingMode.HALF_UP);
    }
}
