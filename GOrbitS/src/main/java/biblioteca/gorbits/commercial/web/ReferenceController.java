package biblioteca.gorbits.commercial.web;

import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.commercial.dto.CampaignResponse;
import biblioteca.gorbits.commercial.dto.ZoneResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    private final CommercialService commercialService;

    public ReferenceController(CommercialService commercialService) {
        this.commercialService = commercialService;
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public List<ZoneResponse> zones() {
        return commercialService.listZones();
    }

    @GetMapping("/campaigns")
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public List<CampaignResponse> campaigns() {
        return commercialService.listCampaigns();
    }
}
