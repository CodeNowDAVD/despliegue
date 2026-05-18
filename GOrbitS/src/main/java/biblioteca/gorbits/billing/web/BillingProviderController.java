package biblioteca.gorbits.billing.web;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.billing.dto.ClientBillingSummaryResponse;
import biblioteca.gorbits.billing.dto.InstallmentRescheduleHistoryItemResponse;
import biblioteca.gorbits.billing.dto.PastDueCollectionItemResponse;
import biblioteca.gorbits.billing.dto.ProviderBillingTotalsResponse;
import biblioteca.gorbits.commercial.CurrentUserService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@PreAuthorize("hasRole('PROVEEDOR')")
public class BillingProviderController {

    private final BillingService billingService;
    private final CurrentUserService currentUser;

    public BillingProviderController(BillingService billingService, CurrentUserService currentUser) {
        this.billingService = billingService;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public ProviderBillingTotalsResponse summary(@RequestParam(required = false) Long campaignId) {
        return billingService.providerTotals(currentUser.requireProveedor(), campaignId);
    }

    @GetMapping("/collections/past-due")
    public List<PastDueCollectionItemResponse> pastDueCollections(
            @RequestParam(required = false) Long campaignId) {
        return billingService.pastDueCollections(currentUser.requireProveedor(), campaignId);
    }

    @GetMapping("/installments/{installmentId}/reschedules")
    public List<InstallmentRescheduleHistoryItemResponse> rescheduleHistory(@PathVariable Long installmentId) {
        return billingService.rescheduleHistory(currentUser.requireProveedor(), installmentId);
    }
}
