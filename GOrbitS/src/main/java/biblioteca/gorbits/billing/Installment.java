package biblioteca.gorbits.billing;

import biblioteca.gorbits.commercial.SalesGuide;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "installments",
        uniqueConstraints = @UniqueConstraint(name = "uk_installment_guide_seq", columnNames = {"guide_id", "seq"}))
public class Installment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private SalesGuide guide;

    /** Orden dentro de la guía: 1..N */
    @Column(nullable = false)
    private int seq;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @OneToMany(mappedBy = "installment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private List<InstallmentPayment> payments = new ArrayList<>();

    protected Installment() {
    }

    public Installment(SalesGuide guide, int seq, LocalDate dueDate, BigDecimal amount) {
        this.guide = guide;
        this.seq = seq;
        this.dueDate = dueDate;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public SalesGuide getGuide() {
        return guide;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public List<InstallmentPayment> getPayments() {
        return payments;
    }

    public void addPayment(BigDecimal amount, LocalDate paidOn, String note) {
        InstallmentPayment p = new InstallmentPayment(this, amount, paidOn, note);
        this.payments.add(p);
    }
}
