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

@Entity
@Table(name = "library_stock_return_lines")
public class LibraryStockReturnLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "library_stock_return_id", nullable = false)
    private LibraryStockReturn parentReturn;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_line_id", nullable = false)
    private LibrarySupplyInvoiceLine invoiceLine;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    protected LibraryStockReturnLine() {}

    public LibraryStockReturnLine(
            LibraryStockReturn parentReturn, LibrarySupplyInvoiceLine invoiceLine, Book book, int quantity) {
        this.parentReturn = parentReturn;
        this.invoiceLine = invoiceLine;
        this.book = book;
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public Book getBook() {
        return book;
    }

    public LibrarySupplyInvoiceLine getInvoiceLine() {
        return invoiceLine;
    }
}
