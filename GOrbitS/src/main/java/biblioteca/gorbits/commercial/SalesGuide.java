package biblioteca.gorbits.commercial;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "sales_guides",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_sales_guide_owner_contract",
                        columnNames = {"owner_id", "contract_number"}))
public class SalesGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GuideStatus status;

    /** Número de contrato del talonario (6 dígitos), único por proveedor. */
    @Column(name = "contract_number", nullable = false, length = 6)
    private String contractNumber;

    /** Fecha del pedido según el talonario físico. */
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String note;

    /** Cuándo el proveedor registró la devolución por parte del cliente (guía en estado DEVUELTA). */
    @Column(name = "client_return_at")
    private Instant clientReturnAt;

    @Column(name = "client_return_reason", length = 500)
    private String clientReturnReason;

    /**
     * Si es true, la guía no aparece en el listado dedicado de devoluciones (sigue persistida y en el listado general de guías).
     */
    @Column(name = "client_return_hidden", nullable = false)
    private boolean clientReturnHidden;

    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<SalesGuideLine> lines = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "sales_guide_tags",
            joinColumns = @JoinColumn(name = "guide_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<SalesContractTag> tags = new HashSet<>();

    protected SalesGuide() {
    }

    public SalesGuide(
            UserAccount owner,
            Campaign campaign,
            Client client,
            GuideStatus status,
            String contractNumber,
            LocalDate orderDate,
            Instant createdAt,
            String note) {
        this.owner = owner;
        this.campaign = campaign;
        this.client = client;
        this.status = status;
        this.contractNumber = contractNumber;
        this.orderDate = orderDate;
        this.createdAt = createdAt;
        this.note = note;
    }

    public void addLine(Book book, int quantity, BigDecimal unitPrice) {
        SalesGuideLine line = new SalesGuideLine(this, book, quantity, unitPrice);
        this.lines.add(line);
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

    public Client getClient() {
        return client;
    }

    public GuideStatus getStatus() {
        return status;
    }

    public void setStatus(GuideStatus status) {
        this.status = status;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<SalesGuideLine> getLines() {
        return lines;
    }

    public Instant getClientReturnAt() {
        return clientReturnAt;
    }

    public String getClientReturnReason() {
        return clientReturnReason;
    }

    public boolean isClientReturnHidden() {
        return clientReturnHidden;
    }

    public void setClientReturnMeta(Instant at, String reason, boolean hidden) {
        this.clientReturnAt = at;
        this.clientReturnReason = reason;
        this.clientReturnHidden = hidden;
    }

    public Set<SalesContractTag> getTags() {
        return tags;
    }

    public void setTags(Set<SalesContractTag> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
}
