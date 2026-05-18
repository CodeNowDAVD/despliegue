package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "library_supply_invoice_lines")
public class LibrarySupplyInvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private LibrarySupplyInvoice invoice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    /** Importe total de la línea (según factura de la librería). */
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    protected LibrarySupplyInvoiceLine() {}

    public LibrarySupplyInvoiceLine(LibrarySupplyInvoice invoice, Book book, int quantity, BigDecimal lineTotal) {
        this.invoice = invoice;
        this.book = book;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public Long getId() {
        return id;
    }

    public LibrarySupplyInvoice getInvoice() {
        return invoice;
    }

    public Book getBook() {
        return book;
    }

    public int getQuantity() {
        return quantity;
    }
}
