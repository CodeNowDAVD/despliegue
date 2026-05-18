package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "provider_field_stock",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_provider_field_stock_owner_book", columnNames = {"owner_id", "book_id"}))
public class ProviderFieldStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    protected ProviderFieldStock() {}

    public ProviderFieldStock(UserAccount owner, Book book, int quantity) {
        this.owner = owner;
        this.book = book;
        this.quantity = quantity;
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

    public int getQuantity() {
        return quantity;
    }

    public void addQuantity(int delta) {
        this.quantity += delta;
    }
}
