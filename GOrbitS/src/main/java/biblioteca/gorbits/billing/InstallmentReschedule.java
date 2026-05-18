package biblioteca.gorbits.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "installment_reschedules")
public class InstallmentReschedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private Installment installment;

    @Column(name = "previous_due_date", nullable = false)
    private LocalDate previousDueDate;

    @Column(name = "new_due_date", nullable = false)
    private LocalDate newDueDate;

    @Column(name = "rescheduled_at", nullable = false)
    private Instant rescheduledAt;

    protected InstallmentReschedule() {}

    public InstallmentReschedule(
            Installment installment, LocalDate previousDueDate, LocalDate newDueDate, Instant rescheduledAt) {
        this.installment = installment;
        this.previousDueDate = previousDueDate;
        this.newDueDate = newDueDate;
        this.rescheduledAt = rescheduledAt;
    }

    public Long getId() {
        return id;
    }

    public Installment getInstallment() {
        return installment;
    }

    public LocalDate getPreviousDueDate() {
        return previousDueDate;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public Instant getRescheduledAt() {
        return rescheduledAt;
    }
}
