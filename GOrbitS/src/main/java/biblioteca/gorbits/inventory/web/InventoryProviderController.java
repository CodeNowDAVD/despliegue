package biblioteca.gorbits.inventory.web;

import biblioteca.gorbits.commercial.CurrentUserService;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.CategoryStockSummaryResponse;
import biblioteca.gorbits.inventory.dto.InventoryMovementResponse;
import biblioteca.gorbits.inventory.dto.ProviderBookStockRowResponse;
import biblioteca.gorbits.inventory.dto.CreateLibraryStockReturnRequest;
import biblioteca.gorbits.inventory.dto.LibraryReconciliationSummaryResponse;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnDetailResponse;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnListItemResponse;
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
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasRole('PROVEEDOR')")
public class InventoryProviderController {

    private final InventoryService inventoryService;
    private final CurrentUserService currentUser;

    public InventoryProviderController(InventoryService inventoryService, CurrentUserService currentUser) {
        this.inventoryService = inventoryService;
        this.currentUser = currentUser;
    }

    @GetMapping("/my-stock")
    public List<ProviderBookStockRowResponse> myStock() {
        return inventoryService.listProviderBookStock(currentUser.requireProveedor());
    }

    @GetMapping("/movements")
    public List<InventoryMovementResponse> listMovements() {
        return inventoryService.listMovements(currentUser.requireProveedor());
    }

    @GetMapping("/stock/by-category")
    public List<CategoryStockSummaryResponse> stockByCategory() {
        return inventoryService.stockSummaryByCategory(currentUser.requireProveedor());
    }

    @GetMapping("/library-stock-returns")
    public List<LibraryStockReturnListItemResponse> listLibraryStockReturns() {
        return inventoryService.listLibraryStockReturns(currentUser.requireProveedor());
    }

    @GetMapping("/library-stock-returns/{id}")
    public LibraryStockReturnDetailResponse getLibraryStockReturn(@PathVariable Long id) {
        return inventoryService.getLibraryStockReturn(currentUser.requireProveedor(), id);
    }

    @PostMapping("/library-stock-returns")
    public ResponseEntity<LibraryStockReturnDetailResponse> createLibraryStockReturn(
            @RequestBody @Valid CreateLibraryStockReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.registerLibraryStockReturn(currentUser.requireProveedor(), request));
    }

    @GetMapping("/library-reconciliation/summary")
    public LibraryReconciliationSummaryResponse reconciliationSummary(
            @RequestParam(required = false) Long campaignId) {
        return inventoryService.libraryReconciliationSummary(currentUser.requireProveedor(), campaignId);
    }
}
