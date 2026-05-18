package biblioteca.gorbits.inventory.dto;

import biblioteca.gorbits.inventory.InventoryMovementType;
import java.time.Instant;

public record InventoryMovementResponse(
        Long id,
        Long bookId,
        String bookTitle,
        InventoryMovementType movementType,
        int quantityDelta,
        int warehouseDelta,
        int fieldDelta,
        String referenceType,
        Long referenceId,
        String note,
        Instant occurredAt
) {
}
