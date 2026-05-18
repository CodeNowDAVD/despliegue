package biblioteca.gorbits.unit.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.billing.Installment;
import biblioteca.gorbits.billing.InstallmentPayment;
import biblioteca.gorbits.billing.InstallmentRepository;
import biblioteca.gorbits.billing.InstallmentRescheduleRepository;
import biblioteca.gorbits.billing.dto.RegisterPaymentRequest;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.ClientRepository;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.config.ClientPaymentMessageProperties;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prueba unitaria de {@link BillingService}: casos de éxito, lista vacía y excepciones.
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceUnitTest {

    @Mock
    SalesGuideRepository guides;

    @Mock
    InstallmentRepository installments;

    @Mock
    InstallmentRescheduleRepository reschedules;

    @Mock
    CampaignRepository campaigns;

    @Mock
    ClientRepository clients;

    @Mock
    ClientPaymentMessageProperties clientPaymentMessage;

    @InjectMocks
    BillingService service;

    private UserAccount owner;

    @BeforeEach
    void setUp() {
        owner = UnitTestFixtures.proveedor(1L);
    }

    @Test
    void providerTotals_sinCuotasPendientes_totalesEnCero() {
        when(installments.findForOwnerBilling(1L, null)).thenReturn(Collections.emptyList());

        var totals = service.providerTotals(owner, null);

        assertThat(totals.totalToCollect()).isEqualByComparingTo("0.00");
        assertThat(totals.pendingInstallmentCount()).isZero();
        verify(installments).findForOwnerBilling(1L, null);
    }

    @Test
    void providerTotals_campanaInexistente_lanzaResourceNotFound() {
        when(campaigns.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.providerTotals(owner, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaña");

        verify(installments, never()).findForOwnerBilling(any(), any());
    }

    @Test
    void listForGuide_guiaNoExiste_lanzaResourceNotFound() {
        when(guides.existsByIdAndOwner_Id(99L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.listForGuide(owner, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerPayment_montoSuperaSaldo_lanzaIllegalArgument() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(1L, owner, "Cli");
        SalesGuide guide = UnitTestFixtures.guide(10L, owner, campaign, client, GuideStatus.ACTIVA);
        var cat = UnitTestFixtures.category(1L, "Cat");
        guide.addLine(UnitTestFixtures.book(1L, cat, "Libro", new BigDecimal("50.00")), 1, new BigDecimal("50.00"));

        Installment inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));

        var req = new RegisterPaymentRequest(new BigDecimal("60.00"), LocalDate.now(), null);
        assertThatThrownBy(() -> service.registerPayment(owner, 100L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saldo pendiente");
    }

    @Test
    void calendar_rangoFechasInvalido_lanza() {
        assertThatThrownBy(() -> service.calendar(owner, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rango");
    }

    @Test
    void clientSummary_campanaNoEncontrada() {
        var client = UnitTestFixtures.client(2L, owner, "Ana");
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(campaigns.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.clientSummary(owner, 2L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaña");
    }

    @Test
    void clientSummary_conCuotasPendientesYSinCampaña() {
        var client = UnitTestFixtures.client(2L, owner, "Ana");
        var campaign = UnitTestFixtures.campaign(1L, "Camp");
        var guide = UnitTestFixtures.guide(10L, owner, campaign, client, GuideStatus.ACTIVA);
        var pendiente = new Installment(guide, 1, LocalDate.now().plusDays(5), new BigDecimal("100.00"));
        var pagada = new Installment(guide, 2, LocalDate.now().minusDays(1), new BigDecimal("50.00"));
        pagada.getPayments().add(new InstallmentPayment(pagada, new BigDecimal("50.00"), LocalDate.now(), null));

        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(installments.findForClientBilling(1L, 2L, null)).thenReturn(List.of(pendiente, pagada));
        when(guides.countByOwner_IdAndClient_Id(1L, 2L)).thenReturn(2L);

        var summary = service.clientSummary(owner, 2L, null);

        assertThat(summary.pendingInstallmentCount()).isEqualTo(1);
        assertThat(summary.totalPending()).isEqualByComparingTo("100.00");
        assertThat(summary.totalCollected()).isEqualByComparingTo("50.00");
        assertThat(summary.guideCount()).isEqualTo(2);
    }
}
