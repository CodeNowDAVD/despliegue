package biblioteca.gorbits.inventory;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.commercial.Campaign;
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
@Table(name = "library_stock_returns")
public class LibraryStockReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "parentReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<LibraryStockReturnLine> lines = new ArrayList<>();

    protected LibraryStockReturn() {}

    public LibraryStockReturn(UserAccount owner, Campaign campaign, Instant createdAt, String note) {
        this.owner = owner;
        this.campaign = campaign;
        this.createdAt = createdAt;
        this.note = note;
    }

    public void addLine(LibrarySupplyInvoiceLine invoiceLine, Book book, int quantity) {
        this.lines.add(new LibraryStockReturnLine(this, invoiceLine, book, quantity));
    }

    public Long getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }

    public List<LibraryStockReturnLine> getLines() {
        return lines;
    }
}
