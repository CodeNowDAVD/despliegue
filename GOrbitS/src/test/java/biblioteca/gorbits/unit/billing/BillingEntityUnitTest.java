package biblioteca.gorbits.unit.billing;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.billing.Installment;
import biblioteca.gorbits.billing.InstallmentPayment;
import biblioteca.gorbits.billing.InstallmentReschedule;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Cubre getters de entidades del paquete billing. */
class BillingEntityUnitTest {

    @Test
    void installmentPayment_getters() {
        var owner = UnitTestFixtures.proveedor(1L);
        var guide = UnitTestFixtures.guide(
                10L,
                owner,
                UnitTestFixtures.campaign(1L, "C"),
                UnitTestFixtures.client(2L, owner, "Cli"),
                GuideStatus.ACTIVA);
        var installment = new Installment(guide, 1, LocalDate.of(2026, 4, 1), new BigDecimal("50.00"));
        var payment = new InstallmentPayment(installment, new BigDecimal("25.00"), LocalDate.of(2026, 4, 10), "abono");
        UnitTestFixtures.setId(payment, 99L);

        assertThat(payment.getId()).isEqualTo(99L);
        assertThat(payment.getInstallment()).isSameAs(installment);
        assertThat(payment.getAmount()).isEqualByComparingTo("25.00");
        assertThat(payment.getPaidOn()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(payment.getNote()).isEqualTo("abono");
    }

    @Test
    void installmentReschedule_getters() {
        var owner = UnitTestFixtures.proveedor(1L);
        var guide = UnitTestFixtures.guide(
                10L,
                owner,
                UnitTestFixtures.campaign(1L, "C"),
                UnitTestFixtures.client(2L, owner, "Cli"),
                GuideStatus.ACTIVA);
        var installment = new Installment(guide, 1, LocalDate.of(2026, 4, 1), new BigDecimal("50.00"));
        var reschedule = new InstallmentReschedule(
                installment,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                Instant.parse("2026-05-01T12:00:00Z"));
        UnitTestFixtures.setId(reschedule, 7L);

        assertThat(reschedule.getId()).isEqualTo(7L);
        assertThat(reschedule.getInstallment()).isSameAs(installment);
        assertThat(reschedule.getPreviousDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(reschedule.getNewDueDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(reschedule.getRescheduledAt()).isEqualTo(Instant.parse("2026-05-01T12:00:00Z"));
    }
}
