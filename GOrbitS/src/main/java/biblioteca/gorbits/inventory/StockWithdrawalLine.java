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
@Table(name = "stock_withdrawal_lines")
public class StockWithdrawalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_id", nullable = false)
    private StockWithdrawal withdrawal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private int quantity;

    protected StockWithdrawalLine() {}

    public StockWithdrawalLine(StockWithdrawal withdrawal, Book book, int quantity) {
        this.withdrawal = withdrawal;
        this.book = book;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public StockWithdrawal getWithdrawal() {
        return withdrawal;
    }

    public Book getBook() {
        return book;
    }

    public int getQuantity() {
        return quantity;
    }
}
