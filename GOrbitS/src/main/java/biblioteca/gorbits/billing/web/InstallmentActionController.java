package biblioteca.gorbits.billing.web;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.billing.dto.InstallmentResponse;
import biblioteca.gorbits.billing.dto.RegisterPaymentRequest;
import biblioteca.gorbits.billing.dto.RegisterPaymentResponse;
import biblioteca.gorbits.billing.dto.RescheduleInstallmentRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/installments")
@PreAuthorize("hasRole('PROVEEDOR')")
public class InstallmentActionController {

    private final BillingService billingService;
    private final CurrentUserService currentUser;

    public InstallmentActionController(BillingService billingService, CurrentUserService currentUser) {
        this.billingService = billingService;
        this.currentUser = currentUser;
    }

    @PatchMapping("/{installmentId}/due-date")
    public InstallmentResponse reschedule(
            @PathVariable Long installmentId, @RequestBody @Valid RescheduleInstallmentRequest body) {
        return billingService.reschedule(currentUser.requireProveedor(), installmentId, body);
    }

    @PostMapping("/{installmentId}/payments")
    public RegisterPaymentResponse pay(
            @PathVariable Long installmentId, @RequestBody @Valid RegisterPaymentRequest body) {
        return billingService.registerPayment(currentUser.requireProveedor(), installmentId, body);
    }
}
