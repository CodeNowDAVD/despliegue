package biblioteca.gorbits.commercial.web;

import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.commercial.dto.ProviderProfileResponse;
import biblioteca.gorbits.commercial.dto.UpdateProviderProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/provider-profile")
@PreAuthorize("hasRole('PROVEEDOR')")
public class MeProviderProfileController {

    private final CommercialService commercialService;
    private final CurrentUserService currentUser;

    public MeProviderProfileController(CommercialService commercialService, CurrentUserService currentUser) {
        this.commercialService = commercialService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ProviderProfileResponse get() {
        return commercialService.getProviderProfile(currentUser.requireProveedor());
    }

    @PutMapping
    public ProviderProfileResponse put(@RequestBody @Valid UpdateProviderProfileRequest request) {
        return commercialService.updateProviderProfile(currentUser.requireProveedor(), request);
    }
}
