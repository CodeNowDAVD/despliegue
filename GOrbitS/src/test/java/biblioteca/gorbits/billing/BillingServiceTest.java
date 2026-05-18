package biblioteca.gorbits.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.billing.dto.CustomInstallmentItem;
import biblioteca.gorbits.billing.dto.CustomInstallmentPlanRequest;
import biblioteca.gorbits.billing.dto.RegisterPaymentRequest;
import biblioteca.gorbits.billing.dto.RescheduleInstallmentRequest;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.Client;
import biblioteca.gorbits.commercial.ClientRepository;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesGuideLine;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.config.ClientPaymentMessageProperties;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

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

    private final UserAccount owner = UnitTestFixtures.proveedor(1L);
    private SalesGuide guide;

    @BeforeEach
    void setUp() {
        var campaign = UnitTestFixtures.campaign(1L, "Camp");
        var client = UnitTestFixtures.client(1L, owner, "Cliente");
        guide = UnitTestFixtures.guide(10L, owner, campaign, client, GuideStatus.ACTIVA);
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(1L, cat, "Libro", new BigDecimal("100.00"));
        guide.addLine(book, 1, new BigDecimal("100.00"));
    }

    @Test
    void createCustomPlan_validaSumaIgualAlTotal() {
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(guide));
        when(installments.existsByGuide_Id(10L)).thenReturn(false);

        var req = new CustomInstallmentPlanRequest(List.of(
                new CustomInstallmentItem(LocalDate.of(2026, 4, 1), new BigDecimal("40.00")),
                new CustomInstallmentItem(LocalDate.of(2026, 5, 1), new BigDecimal("50.00"))));

        assertThatThrownBy(() -> service.createCustomPlan(owner, 10L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe igualar");
    }

    @Test
    void createCustomPlan_rechazaGuiaDevuelta() {
        guide.setStatus(GuideStatus.DEVUELTA);
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(guide));

        var req = new CustomInstallmentPlanRequest(
                List.of(new CustomInstallmentItem(LocalDate.of(2026, 4, 1), new BigDecimal("100.00"))));
        assertThatThrownBy(() -> service.createCustomPlan(owner, 10L, req)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createCustomPlan_guardaCuotasOrdenadas() {
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(guide));
        when(installments.existsByGuide_Id(10L)).thenReturn(false);
        when(installments.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(installments.findByGuide_IdOrderBySeqAsc(10L)).thenAnswer(inv -> List.of());

        var req = new CustomInstallmentPlanRequest(List.of(
                new CustomInstallmentItem(LocalDate.of(2026, 5, 1), new BigDecimal("60.00")),
                new CustomInstallmentItem(LocalDate.of(2026, 4, 1), new BigDecimal("40.00"))));
        service.createCustomPlan(owner, 10L, req);

        verify(installments).saveAll(any());
    }

    @Test
    void registerPayment_rechazaMontoMayorAlSaldo() {
        var inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));

        var req = new RegisterPaymentRequest(new BigDecimal("60.00"), LocalDate.now(), null);
        assertThatThrownBy(() -> service.registerPayment(owner, 100L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("saldo pendiente");
    }

    @Test
    void calendar_rechazaRangoInvalido() {
        assertThatThrownBy(() -> service.calendar(owner, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void providerTotals_sinCampaña_sumaCuotas() {
        var inst = new Installment(guide, 1, LocalDate.now().plusDays(5), new BigDecimal("100.00"));
        when(installments.findForOwnerBilling(1L, null)).thenReturn(List.of(inst));

        var totals = service.providerTotals(owner, null);

        assertThat(totals.totalToCollect()).isEqualByComparingTo("100.00");
        assertThat(totals.totalPending()).isEqualByComparingTo("100.00");
        assertThat(totals.pendingInstallmentCount()).isEqualTo(1);
    }

    @Test
    void listForGuide_guiaAjena() {
        when(guides.existsByIdAndOwner_Id(99L, 1L)).thenReturn(false);
        assertThatThrownBy(() -> service.listForGuide(owner, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void clientSummary_clienteNoEncontrado() {
        when(clients.findByIdAndOwner_Id(5L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.clientSummary(owner, 5L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listForGuide_exito() {
        when(guides.existsByIdAndOwner_Id(10L, 1L)).thenReturn(true);
        var inst = new Installment(guide, 1, LocalDate.of(2026, 4, 1), new BigDecimal("50.00"));
        when(installments.findByGuide_IdOrderBySeqAsc(10L)).thenReturn(List.of(inst));

        assertThat(service.listForGuide(owner, 10L)).hasSize(1).first().satisfies(i -> assertThat(i.sequence()).isEqualTo(1));
    }

    @Test
    void createCustomPlan_guiaNoEncontrada() {
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.empty());
        var req = new CustomInstallmentPlanRequest(
                List.of(new CustomInstallmentItem(LocalDate.of(2026, 4, 1), new BigDecimal("100.00"))));
        assertThatThrownBy(() -> service.createCustomPlan(owner, 10L, req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createCustomPlan_yaTieneCuotas() {
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(guide));
        when(installments.existsByGuide_Id(10L)).thenReturn(true);
        var req = new CustomInstallmentPlanRequest(
                List.of(new CustomInstallmentItem(LocalDate.of(2026, 4, 1), new BigDecimal("100.00"))));
        assertThatThrownBy(() -> service.createCustomPlan(owner, 10L, req)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reschedule_cambiaFecha_guardaHistorial() {
        var inst = new Installment(guide, 1, LocalDate.of(2026, 4, 1), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));

        service.reschedule(owner, 100L, new RescheduleInstallmentRequest(LocalDate.of(2026, 5, 15)));

        verify(reschedules).save(any(InstallmentReschedule.class));
        verify(installments).save(inst);
    }

    @Test
    void reschedule_mismaFecha_noDuplicaHistorial() {
        var due = LocalDate.of(2026, 4, 1);
        var inst = new Installment(guide, 1, due, new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));

        service.reschedule(owner, 100L, new RescheduleInstallmentRequest(due));

        verify(reschedules, never()).save(any());
        verify(installments, never()).save(any());
    }

    @Test
    void reschedule_detalleTrasGuardarNoEncontrado() {
        var inst = new Installment(guide, 1, LocalDate.of(2026, 4, 1), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reschedule(owner, 100L, new RescheduleInstallmentRequest(LocalDate.of(2026, 6, 1))))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void reschedule_cuotaNoEncontrada() {
        when(installments.findByIdAndOwner_Id(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reschedule(owner, 99L, new RescheduleInstallmentRequest(LocalDate.now())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerPayment_notaConTexto_seGuardaTrim() {
        var inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst), Optional.of(inst));
        when(clientPaymentMessage.template()).thenReturn("{cliente}");
        when(clientPaymentMessage.currencySymbol()).thenReturn("S/");
        when(installments.findByGuide_IdOrderBySeqAsc(10L)).thenReturn(List.of(inst));

        service.registerPayment(
                owner, 100L, new RegisterPaymentRequest(new BigDecimal("10.00"), LocalDate.now(), "  observación  "));

        assertThat(inst.getPayments().getFirst().getNote()).isEqualTo("observación");
    }

    @Test
    void registerPayment_parcial_conMensajeYNotaVacia() {
        var inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("100.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst), Optional.of(inst));
        when(clientPaymentMessage.template()).thenReturn("Cliente {cliente} abono {moneda}{abono}");
        when(clientPaymentMessage.currencySymbol()).thenReturn("S/");
        when(installments.findByGuide_IdOrderBySeqAsc(10L)).thenReturn(List.of(inst));

        var res = service.registerPayment(
                owner, 100L, new RegisterPaymentRequest(new BigDecimal("40.00"), LocalDate.of(2026, 3, 10), "   "));

        assertThat(res.installment().remaining()).isEqualByComparingTo("60.00");
        assertThat(res.clientMessage()).contains("Cliente");
        assertThat(inst.getPayments().getFirst().getNote()).isNull();
    }

    @Test
    void registerPayment_cuotaNoEncontrada() {
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerPayment(
                        owner, 100L, new RegisterPaymentRequest(new BigDecimal("10.00"), LocalDate.now(), null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cuota no encontrada");
    }

    @Test
    void registerPayment_cuotaYaPagada() {
        var inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        inst.addPayment(new BigDecimal("50.00"), LocalDate.now(), "completo");
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));

        assertThatThrownBy(() -> service.registerPayment(
                        owner, 100L, new RegisterPaymentRequest(new BigDecimal("1.00"), LocalDate.now(), null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pagada");
    }

    @Test
    void registerPayment_recargaFalla() {
        var inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        when(installments.findDetailByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst), Optional.empty());

        assertThatThrownBy(() -> service.registerPayment(
                        owner, 100L, new RegisterPaymentRequest(new BigDecimal("10.00"), LocalDate.now(), "nota")))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void rescheduleHistory_listaCambios() {
        var inst = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        UnitTestFixtures.setId(inst, 100L);
        var hist = new InstallmentReschedule(
                inst, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), Instant.parse("2026-02-01T12:00:00Z"));
        when(installments.findByIdAndOwner_Id(100L, 1L)).thenReturn(Optional.of(inst));
        when(reschedules.findByInstallment_IdOrderByRescheduledAtDesc(100L)).thenReturn(List.of(hist));

        assertThat(service.rescheduleHistory(owner, 100L)).hasSize(1).first().satisfies(h -> assertThat(h.newDueDate())
                .isEqualTo(LocalDate.of(2026, 2, 1)));
    }

    @Test
    void rescheduleHistory_cuotaNoEncontrada() {
        when(installments.findByIdAndOwner_Id(88L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rescheduleHistory(owner, 88L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pastDueCollections_soloConSaldoPendiente() {
        var vencida = new Installment(guide, 1, LocalDate.now().minusDays(7), new BigDecimal("80.00"));
        var vencidaPagada = new Installment(guide, 2, LocalDate.now().minusDays(3), new BigDecimal("20.00"));
        vencidaPagada.addPayment(new BigDecimal("20.00"), LocalDate.now(), null);
        var futura = new Installment(guide, 3, LocalDate.now().plusDays(5), new BigDecimal("15.00"));
        when(installments.findForOwnerBilling(1L, null)).thenReturn(List.of(vencida, vencidaPagada, futura));

        var list = service.pastDueCollections(owner, null);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().pendingAmount()).isEqualByComparingTo("80.00");
        assertThat(list.getFirst().daysPastDue()).isGreaterThan(0);
    }

    @Test
    void pastDueCollections_conCampanaValida() {
        when(campaigns.existsById(5L)).thenReturn(true);
        var vencida = new Installment(guide, 1, LocalDate.now().minusDays(4), new BigDecimal("40.00"));
        when(installments.findForOwnerBilling(1L, 5L)).thenReturn(List.of(vencida));

        var list = service.pastDueCollections(owner, 5L);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().contractNumber()).isEqualTo(guide.getContractNumber());
    }

    @Test
    void pastDueCollections_campanaInexistente() {
        when(campaigns.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.pastDueCollections(owner, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calendar_omiteCuotasLiquidadas() {
        var pagada = new Installment(guide, 1, LocalDate.now(), new BigDecimal("50.00"));
        pagada.addPayment(new BigDecimal("50.00"), LocalDate.now(), null);
        var pendiente = new Installment(guide, 2, LocalDate.now().plusDays(2), new BigDecimal("30.00"));
        when(installments.findDueBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(pagada, pendiente));

        var days = service.calendar(owner, LocalDate.now(), LocalDate.now().plusDays(10));

        assertThat(days).hasSize(1);
        assertThat(days.getFirst().items()).hasSize(1);
    }

    @Test
    void providerTotals_cuentaCuotasVencidas() {
        var vencida = new Installment(guide, 1, LocalDate.now().minusDays(4), new BigDecimal("100.00"));
        when(installments.findForOwnerBilling(1L, 1L)).thenReturn(List.of(vencida));
        when(campaigns.existsById(1L)).thenReturn(true);
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "Camp")));

        var totals = service.providerTotals(owner, 1L);

        assertThat(totals.pastDueInstallmentCount()).isEqualTo(1);
        assertThat(totals.campaignName()).isEqualTo("Camp");
    }

    @Test
    void clientSummary_conCampaña() {
        var client = UnitTestFixtures.client(2L, owner, "Ana");
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(campaigns.existsById(1L)).thenReturn(true);
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "Verano")));
        when(installments.findForClientBilling(1L, 2L, 1L)).thenReturn(List.of());
        when(guides.countByOwner_IdAndClient_IdAndCampaign_Id(1L, 2L, 1L)).thenReturn(3L);

        var summary = service.clientSummary(owner, 2L, 1L);

        assertThat(summary.guideCount()).isEqualTo(3);
        assertThat(summary.campaignName()).isEqualTo("Verano");
    }
}
