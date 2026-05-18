package biblioteca.gorbits.inventory.dto;

import jakarta.validation.constraints.Min;

public record SetWarehouseStockRequest(@Min(0) int quantity) {}
