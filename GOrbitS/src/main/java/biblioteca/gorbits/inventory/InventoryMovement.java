package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InventoryMovementType movementType;

    /** Positivo = entrada al stock del proveedor; negativo = salida. */
    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "warehouse_delta", nullable = false)
    private int warehouseDelta;

    @Column(name = "field_delta", nullable = false)
    private int fieldDelta;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 500)
    private String note;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected InventoryMovement() {}

    public InventoryMovement(
            UserAccount owner,
            Book book,
            InventoryMovementType movementType,
            int quantityDelta,
            int warehouseDelta,
            int fieldDelta,
            String referenceType,
            Long referenceId,
            String note,
            Instant occurredAt) {
        this.owner = owner;
        this.book = book;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.warehouseDelta = warehouseDelta;
        this.fieldDelta = fieldDelta;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public Book getBook() {
        return book;
    }

    public InventoryMovementType getMovementType() {
        return movementType;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getWarehouseDelta() {
        return warehouseDelta;
    }

    public int getFieldDelta() {
        return fieldDelta;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getNote() {
        return note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
