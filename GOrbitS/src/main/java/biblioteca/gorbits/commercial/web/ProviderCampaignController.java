package biblioteca.gorbits.commercial.web;

import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.commercial.ProviderReportingService;
import biblioteca.gorbits.commercial.dto.CampaignCloseReportResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/provider/campaigns")
@PreAuthorize("hasRole('PROVEEDOR')")
public class ProviderCampaignController {

    private final ProviderReportingService reporting;
    private final CurrentUserService currentUser;

    public ProviderCampaignController(ProviderReportingService reporting, CurrentUserService currentUser) {
        this.reporting = reporting;
        this.currentUser = currentUser;
    }

    @GetMapping("/{campaignId}/close-report")
    public CampaignCloseReportResponse closeReport(@PathVariable Long campaignId) {
        return reporting.campaignCloseReport(currentUser.requireProveedor(), campaignId);
    }
}
