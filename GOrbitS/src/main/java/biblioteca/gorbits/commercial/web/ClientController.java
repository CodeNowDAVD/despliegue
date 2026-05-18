package biblioteca.gorbits.commercial.web;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.billing.dto.ClientBillingSummaryResponse;
import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.ClientResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@PreAuthorize("hasRole('PROVEEDOR')")
public class ClientController {

    private final CommercialService commercialService;
    private final BillingService billingService;
    private final CurrentUserService currentUser;

    public ClientController(
            CommercialService commercialService, BillingService billingService, CurrentUserService currentUser) {
        this.commercialService = commercialService;
        this.billingService = billingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ClientResponse> list(@RequestParam(required = false) String q) {
        return commercialService.listClients(currentUser.requireProveedor(), q);
    }

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return commercialService.getClient(currentUser.requireProveedor(), id);
    }

    @GetMapping("/{id}/billing-summary")
    public ClientBillingSummaryResponse billingSummary(
            @PathVariable Long id, @RequestParam(required = false) Long campaignId) {
        return billingService.clientSummary(currentUser.requireProveedor(), id, campaignId);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@RequestBody @Valid ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commercialService.createClient(currentUser.requireProveedor(), request));
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable Long id, @RequestBody @Valid ClientRequest request) {
        return commercialService.updateClient(currentUser.requireProveedor(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commercialService.deleteClient(currentUser.requireProveedor(), id);
        return ResponseEntity.noContent().build();
    }
}
