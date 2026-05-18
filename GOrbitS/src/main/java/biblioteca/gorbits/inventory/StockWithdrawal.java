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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_withdrawals")
public class StockWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "withdrawal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<StockWithdrawalLine> lines = new ArrayList<>();

    protected StockWithdrawal() {}

    public StockWithdrawal(UserAccount owner, Instant createdAt, String note) {
        this.owner = owner;
        this.createdAt = createdAt;
        this.note = note;
    }

    public void addLine(Book book, int quantity) {
        this.lines.add(new StockWithdrawalLine(this, book, quantity));
    }

    public Long getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }

    public List<StockWithdrawalLine> getLines() {
        return lines;
    }
}
