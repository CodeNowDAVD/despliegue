package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "library_supply_invoices",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_library_supply_invoice_owner_number",
                        columnNames = {"owner_id", "invoice_number"}))
public class LibrarySupplyInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(name = "invoice_number", nullable = false, length = 80)
    private String invoiceNumber;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<LibrarySupplyInvoiceLine> lines = new ArrayList<>();

    protected LibrarySupplyInvoice() {}

    public LibrarySupplyInvoice(
            UserAccount owner, String invoiceNumber, LocalDate issuedOn, String note, Instant createdAt) {
        this.owner = owner;
        this.invoiceNumber = invoiceNumber;
        this.issuedOn = issuedOn;
        this.note = note;
        this.createdAt = createdAt;
    }

    public void addLine(Book book, int quantity, BigDecimal lineTotal) {
        this.lines.add(new LibrarySupplyInvoiceLine(this, book, quantity, lineTotal));
    }

    public Long getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LibrarySupplyInvoiceLine> getLines() {
        return lines;
    }
}
