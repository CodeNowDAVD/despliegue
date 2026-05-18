package biblioteca.gorbits.inventory.web;

import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.CreateLibrarySupplyInvoiceRequest;
import biblioteca.gorbits.inventory.dto.LibrarySupplyInvoiceDetailResponse;
import biblioteca.gorbits.inventory.dto.LibrarySupplyInvoiceListItemResponse;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/library-invoices")
@PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
public class LibrarySupplyInvoiceController {

    private final InventoryService inventoryService;
    private final CurrentUserService currentUser;

    public LibrarySupplyInvoiceController(InventoryService inventoryService, CurrentUserService currentUser) {
        this.inventoryService = inventoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<LibrarySupplyInvoiceListItemResponse> list(@RequestParam(required = false) Long providerId) {
        UserAccount viewer = currentUser.currentUser();
        Long filter = viewer.getRole() == Role.ADMIN ? providerId : null;
        return inventoryService.listLibrarySupplyInvoices(viewer, filter);
    }

    @GetMapping("/{id}")
    public LibrarySupplyInvoiceDetailResponse get(@PathVariable Long id) {
        return inventoryService.getLibrarySupplyInvoice(currentUser.currentUser(), id);
    }

    @PostMapping
    public ResponseEntity<LibrarySupplyInvoiceDetailResponse> create(
            @RequestBody @Valid CreateLibrarySupplyInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.registerLibrarySupplyInvoice(currentUser.currentUser(), request));
    }
}
