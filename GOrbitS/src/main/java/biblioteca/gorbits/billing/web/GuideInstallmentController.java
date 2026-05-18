package biblioteca.gorbits.billing.web;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.billing.dto.CustomInstallmentPlanRequest;
import biblioteca.gorbits.billing.dto.InstallmentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guides/{guideId}/installments")
@PreAuthorize("hasRole('PROVEEDOR')")
public class GuideInstallmentController {

    private final BillingService billingService;
    private final CurrentUserService currentUser;

    public GuideInstallmentController(BillingService billingService, CurrentUserService currentUser) {
        this.billingService = billingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<InstallmentResponse> list(@PathVariable Long guideId) {
        return billingService.listForGuide(currentUser.requireProveedor(), guideId);
    }

    @PostMapping("/plan/custom")
    public ResponseEntity<List<InstallmentResponse>> createCustom(
            @PathVariable Long guideId, @RequestBody @Valid CustomInstallmentPlanRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.createCustomPlan(currentUser.requireProveedor(), guideId, body));
    }
}
