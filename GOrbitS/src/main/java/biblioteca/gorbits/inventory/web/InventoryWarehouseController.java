package biblioteca.gorbits.inventory.web;

import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.SetWarehouseStockRequest;
import biblioteca.gorbits.inventory.dto.WarehouseStockRowResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/warehouse")
public class InventoryWarehouseController {

    private final InventoryService inventoryService;

    public InventoryWarehouseController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PROVEEDOR')")
    public List<WarehouseStockRowResponse> list() {
        return inventoryService.listWarehouseStock();
    }

    @PutMapping("/books/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void setBookStock(@PathVariable Long bookId, @RequestBody @Valid SetWarehouseStockRequest request) {
        inventoryService.setWarehouseStock(bookId, request);
    }
}
