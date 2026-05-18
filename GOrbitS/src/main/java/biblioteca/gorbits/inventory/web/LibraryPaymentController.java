package biblioteca.gorbits.inventory.web;

import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.CreateLibraryPaymentRequest;
import biblioteca.gorbits.inventory.dto.LibraryPaymentDetailResponse;
import biblioteca.gorbits.inventory.dto.LibraryPaymentListItemResponse;
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
@RequestMapping("/api/v1/inventory/library-payments")
@PreAuthorize("hasRole('PROVEEDOR')")
public class LibraryPaymentController {

    private final InventoryService inventoryService;
    private final CurrentUserService currentUser;

    public LibraryPaymentController(InventoryService inventoryService, CurrentUserService currentUser) {
        this.inventoryService = inventoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<LibraryPaymentListItemResponse> list() {
        return inventoryService.listLibraryPayments(currentUser.requireProveedor());
    }

    @GetMapping("/{id}")
    public LibraryPaymentDetailResponse get(@PathVariable Long id) {
        return inventoryService.getLibraryPayment(currentUser.requireProveedor(), id);
    }

    @PostMapping
    public ResponseEntity<LibraryPaymentDetailResponse> create(@RequestBody @Valid CreateLibraryPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.registerLibraryPayment(currentUser.requireProveedor(), request));
    }
}
