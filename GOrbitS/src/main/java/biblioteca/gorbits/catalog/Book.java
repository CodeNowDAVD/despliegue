package biblioteca.gorbits.catalog;

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
import java.math.BigDecimal;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private BookCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookType bookType;

    /** Explicación opcional del paquete (líneas en factura, qué incluye el “regalo”, etc.). */
    @Column(length = 500)
    private String packageNote;

    /** Libro complementario que se factura como segunda línea en un PAQUETE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_book_id")
    private Book companionBook;

    /** Precio unitario simbólico de la línea del complemento (p. ej. 0.01). */
    @Column(name = "companion_line_price", precision = 12, scale = 2)
    private BigDecimal companionLinePrice;

    protected Book() {
    }

    public Book(BookCategory category, String title, BigDecimal price, BookType bookType, String packageNote) {
        this.category = category;
        this.title = title;
        this.price = price;
        this.bookType = bookType;
        this.packageNote = packageNote;
    }

    public Long getId() {
        return id;
    }

    public BookCategory getCategory() {
        return category;
    }

    public void setCategory(BookCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BookType getBookType() {
        return bookType;
    }

    public void setBookType(BookType bookType) {
        this.bookType = bookType;
    }

    public String getPackageNote() {
        return packageNote;
    }

    public void setPackageNote(String packageNote) {
        this.packageNote = packageNote;
    }

    public Book getCompanionBook() {
        return companionBook;
    }

    public void setCompanionBook(Book companionBook) {
        this.companionBook = companionBook;
    }

    public BigDecimal getCompanionLinePrice() {
        return companionLinePrice;
    }

    public void setCompanionLinePrice(BigDecimal companionLinePrice) {
        this.companionLinePrice = companionLinePrice;
    }
}
