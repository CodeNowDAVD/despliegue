package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.user.UserAccount;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class InventoryMovementLogger {

    private final InventoryMovementRepository movements;

    public InventoryMovementLogger(InventoryMovementRepository movements) {
        this.movements = movements;
    }

    public void log(
            UserAccount owner,
            Book book,
            InventoryMovementType type,
            int warehouseDelta,
            int fieldDelta,
            String referenceType,
            Long referenceId,
            String note,
            Instant occurredAt) {
        int quantityDelta = warehouseDelta + fieldDelta;
        movements.save(
                new InventoryMovement(
                        owner,
                        book,
                        type,
                        quantityDelta,
                        warehouseDelta,
                        fieldDelta,
                        referenceType,
                        referenceId,
                        note,
                        occurredAt));
    }
}
