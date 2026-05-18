package biblioteca.gorbits.billing;

import biblioteca.gorbits.billing.dto.CalendarDayResponse;
import biblioteca.gorbits.billing.dto.ClientBillingSummaryResponse;
import biblioteca.gorbits.billing.dto.CustomInstallmentPlanRequest;
import biblioteca.gorbits.billing.dto.InstallmentCalendarItemResponse;
import biblioteca.gorbits.billing.dto.InstallmentRescheduleHistoryItemResponse;
import biblioteca.gorbits.billing.dto.InstallmentResponse;
import biblioteca.gorbits.billing.dto.PastDueCollectionItemResponse;
import biblioteca.gorbits.billing.dto.ProviderBillingTotalsResponse;
import biblioteca.gorbits.billing.dto.RegisterPaymentRequest;
import biblioteca.gorbits.billing.dto.RegisterPaymentResponse;
import biblioteca.gorbits.billing.dto.RescheduleInstallmentRequest;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.Client;
import biblioteca.gorbits.commercial.ClientRepository;
import biblioteca.gorbits.config.ClientPaymentMessageProperties;
import biblioteca.gorbits.commercial.GuideLifecycleRules;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesGuideLine;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private static final DateTimeFormatter DATE_ES = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SalesGuideRepository guides;
    private final InstallmentRepository installments;
    private final InstallmentRescheduleRepository reschedules;
    private final CampaignRepository campaigns;
    private final ClientRepository clients;
    private final ClientPaymentMessageProperties clientPaymentMessage;

    public BillingService(
            SalesGuideRepository guides,
            InstallmentRepository installments,
            InstallmentRescheduleRepository reschedules,
            CampaignRepository campaigns,
            ClientRepository clients,
            ClientPaymentMessageProperties clientPaymentMessage) {
        this.guides = guides;
        this.installments = installments;
        this.reschedules = reschedules;
        this.campaigns = campaigns;
        this.clients = clients;
        this.clientPaymentMessage = clientPaymentMessage;
    }

    @Transactional(readOnly = true)
    public List<InstallmentResponse> listForGuide(UserAccount owner, Long guideId) {
        if (!guides.existsByIdAndOwner_Id(guideId, owner.getId())) {
            throw new ResourceNotFoundException("Guía no encontrada");
        }
        return installments.findByGuide_IdOrderBySeqAsc(guideId).stream()
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional
    public List<InstallmentResponse> createCustomPlan(UserAccount owner, Long guideId, CustomInstallmentPlanRequest request) {
        SalesGuide guide = loadGuide(owner, guideId);
        GuideLifecycleRules.assertMutable(guide);
        if (installments.existsByGuide_Id(guide.getId())) {
            throw new IllegalStateException("Esta guía ya tiene cuotas definidas");
        }
        BigDecimal total = guideTotal(guide);
        BigDecimal sumItems = request.items().stream()
                .map(i -> i.amount())
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(sumItems) != 0) {
            throw new IllegalArgumentException("La suma de cuotas (" + sumItems + ") debe igualar el total de la guía (" + total + ")");
        }
        List<Installment> created = new ArrayList<>();
        int seq = 1;
        var sorted =
                request.items().stream().sorted(Comparator.comparing(i -> i.dueDate())).toList();
        for (var item : sorted) {
            created.add(new Installment(guide, seq++, item.dueDate(), item.amount().setScale(2, RoundingMode.HALF_UP)));
        }
        installments.saveAll(created);
        return installments.findByGuide_IdOrderBySeqAsc(guide.getId()).stream()
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional
    public InstallmentResponse reschedule(UserAccount owner, Long installmentId, RescheduleInstallmentRequest request) {
        Installment i = installments
                .findByIdAndOwner_Id(installmentId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));
        GuideLifecycleRules.assertMutable(i.getGuide());
        LocalDate previous = i.getDueDate();
        LocalDate next = request.dueDate();
        if (!previous.equals(next)) {
            reschedules.save(new InstallmentReschedule(i, previous, next, Instant.now()));
            i.setDueDate(next);
            installments.save(i);
        }
        return toInstallmentResponse(
                installments.findDetailByIdAndOwner_Id(installmentId, owner.getId()).orElseThrow());
    }

    @Transactional
    public RegisterPaymentResponse registerPayment(UserAccount owner, Long installmentId, RegisterPaymentRequest request) {
        Installment i = installments
                .findDetailByIdAndOwner_Id(installmentId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));
        GuideLifecycleRules.assertMutable(i.getGuide());
        BigDecimal remaining = remaining(i);
        if (remaining.compareTo(ZERO) <= 0) {
            throw new IllegalStateException("La cuota ya está pagada");
        }
        if (request.amount().compareTo(remaining) > 0) {
            throw new IllegalArgumentException("El monto supera el saldo pendiente (" + remaining + ")");
        }
        i.addPayment(
                request.amount().setScale(2, RoundingMode.HALF_UP),
                request.paidOn(),
                emptyToNull(request.note()));
        installments.save(i);
        Installment reloaded = installments
                .findDetailByIdAndOwner_Id(installmentId, owner.getId())
                .orElseThrow();
        InstallmentResponse body = toInstallmentResponse(reloaded);
        String message = buildClientMessage(reloaded, request.amount(), request.paidOn());
        return new RegisterPaymentResponse(body, message);
    }

    @Transactional(readOnly = true)
    public List<CalendarDayResponse> calendar(UserAccount owner, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Rango de fechas inválido");
        }
        List<Installment> list = installments.findDueBetween(owner.getId(), from, to);
        Map<LocalDate, List<InstallmentCalendarItemResponse>> grouped = new LinkedHashMap<>();
        for (Installment i : list) {
            if (remaining(i).compareTo(ZERO) <= 0) {
                continue;
            }
            var item = new InstallmentCalendarItemResponse(
                    i.getId(),
                    i.getGuide().getId(),
                    i.getGuide().getContractNumber(),
                    i.getGuide().getClient().getFullName(),
                    i.getSeq(),
                    i.getDueDate(),
                    i.getAmount(),
                    paidTotal(i),
                    remaining(i));
            grouped.computeIfAbsent(i.getDueDate(), d -> new ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new CalendarDayResponse(
                        e.getKey(), e.getValue().size(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderBillingTotalsResponse providerTotals(UserAccount owner, Long campaignId) {
        if (campaignId != null && !campaigns.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaña no encontrada");
        }
        List<Installment> list = installments.findForOwnerBilling(owner.getId(), campaignId);
        LocalDate today = LocalDate.now();
        BigDecimal toCollect = ZERO;
        BigDecimal collected = ZERO;
        BigDecimal pending = ZERO;
        int pendingCount = 0;
        int pastDueCount = 0;
        for (Installment i : list) {
            BigDecimal paid = paidTotal(i);
            BigDecimal rem = remaining(i);
            toCollect = toCollect.add(i.getAmount());
            collected = collected.add(paid);
            if (rem.compareTo(ZERO) > 0) {
                pending = pending.add(rem);
                pendingCount++;
                if (i.getDueDate().isBefore(today)) {
                    pastDueCount++;
                }
            }
        }
        String campaignName = null;
        if (campaignId != null) {
            campaignName = campaigns.findById(campaignId).map(Campaign::getName).orElse(null);
        }
        return new ProviderBillingTotalsResponse(
                campaignId,
                campaignName,
                toCollect.setScale(2, RoundingMode.HALF_UP),
                collected.setScale(2, RoundingMode.HALF_UP),
                pending.setScale(2, RoundingMode.HALF_UP),
                pendingCount,
                pastDueCount);
    }

    @Transactional(readOnly = true)
    public ClientBillingSummaryResponse clientSummary(UserAccount owner, Long clientId, Long campaignId) {
        Client client = clients
                .findByIdAndOwner_Id(clientId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (campaignId != null && !campaigns.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaña no encontrada");
        }
        List<Installment> list = installments.findForClientBilling(owner.getId(), clientId, campaignId);
        BigDecimal toCollect = ZERO;
        BigDecimal collected = ZERO;
        BigDecimal pending = ZERO;
        int pendingCount = 0;
        for (Installment i : list) {
            BigDecimal paid = paidTotal(i);
            BigDecimal rem = remaining(i);
            toCollect = toCollect.add(i.getAmount());
            collected = collected.add(paid);
            if (rem.compareTo(ZERO) > 0) {
                pending = pending.add(rem);
                pendingCount++;
            }
        }
        int guideCount = campaignId == null
                ? (int) guides.countByOwner_IdAndClient_Id(owner.getId(), clientId)
                : (int) guides.countByOwner_IdAndClient_IdAndCampaign_Id(owner.getId(), clientId, campaignId);
        String campaignName = campaignId == null
                ? null
                : campaigns.findById(campaignId).map(Campaign::getName).orElse(null);
        return new ClientBillingSummaryResponse(
                client.getId(),
                client.getFullName(),
                campaignId,
                campaignName,
                guideCount,
                toCollect.setScale(2, RoundingMode.HALF_UP),
                collected.setScale(2, RoundingMode.HALF_UP),
                pending.setScale(2, RoundingMode.HALF_UP),
                pendingCount);
    }

    @Transactional(readOnly = true)
    public List<PastDueCollectionItemResponse> pastDueCollections(UserAccount owner, Long campaignId) {
        if (campaignId != null && !campaigns.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaña no encontrada");
        }
        LocalDate today = LocalDate.now();
        return installments.findForOwnerBilling(owner.getId(), campaignId).stream()
                .filter(i -> i.getDueDate().isBefore(today))
                .map(i -> {
                    BigDecimal paid = paidTotal(i);
                    BigDecimal rem = remaining(i);
                    return new Object[] {i, paid, rem};
                })
                .filter(arr -> ((BigDecimal) arr[2]).compareTo(ZERO) > 0)
                .map(arr -> {
                    Installment i = (Installment) arr[0];
                    BigDecimal paid = (BigDecimal) arr[1];
                    BigDecimal rem = (BigDecimal) arr[2];
                    long days = ChronoUnit.DAYS.between(i.getDueDate(), today);
                    return new PastDueCollectionItemResponse(
                            i.getId(),
                            i.getGuide().getId(),
                            i.getGuide().getContractNumber(),
                            i.getGuide().getClient().getId(),
                            i.getGuide().getClient().getFullName(),
                            i.getSeq(),
                            i.getDueDate(),
                            i.getAmount(),
                            paid,
                            rem,
                            days);
                })
                .sorted(Comparator.comparing(PastDueCollectionItemResponse::dueDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentRescheduleHistoryItemResponse> rescheduleHistory(UserAccount owner, Long installmentId) {
        installments
                .findByIdAndOwner_Id(installmentId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuota no encontrada"));
        return reschedules.findByInstallment_IdOrderByRescheduledAtDesc(installmentId).stream()
                .map(
                        r -> new InstallmentRescheduleHistoryItemResponse(
                                r.getId(), r.getPreviousDueDate(), r.getNewDueDate(), r.getRescheduledAt()))
                .toList();
    }

    private SalesGuide loadGuide(UserAccount owner, Long guideId) {
        return guides.findDetailedByIdAndOwner_Id(guideId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Guía no encontrada"));
    }

    private BigDecimal guideTotal(SalesGuide guide) {
        BigDecimal t = ZERO;
        for (SalesGuideLine line : guide.getLines()) {
            t = t.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        return t.setScale(2, RoundingMode.HALF_UP);
    }

    private InstallmentResponse toInstallmentResponse(Installment i) {
        BigDecimal paid = paidTotal(i);
        BigDecimal remaining = remaining(i);
        boolean done = remaining.compareTo(ZERO) <= 0;
        return new InstallmentResponse(
                i.getId(),
                i.getGuide().getId(),
                i.getSeq(),
                i.getDueDate(),
                i.getAmount(),
                paid,
                remaining,
                done);
    }

    private BigDecimal paidTotal(Installment i) {
        return i.getPayments().stream()
                .map(InstallmentPayment::getAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal remaining(Installment i) {
        return i.getAmount().subtract(paidTotal(i)).max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String buildClientMessage(Installment installment, BigDecimal abono, LocalDate paidOn) {
        String tpl = clientPaymentMessage.template();
        String sym = clientPaymentMessage.currencySymbol();
        BigDecimal saldoCuota = remaining(installment);
        BigDecimal saldoGuia = guideRemainingTotal(installment.getGuide().getId());
        String cliente = installment.getGuide().getClient().getFullName();
        String campana = installment.getGuide().getCampaign().getName();
        return tpl.replace("{cliente}", cliente)
                .replace("{campana}", campana)
                .replace("{guiaId}", String.valueOf(installment.getGuide().getId()))
                .replace("{cuotaSeq}", String.valueOf(installment.getSeq()))
                .replace("{abono}", formatMoney(abono))
                .replace("{fechaAbono}", paidOn.format(DATE_ES))
                .replace("{saldoCuota}", formatMoney(saldoCuota))
                .replace("{saldoGuia}", formatMoney(saldoGuia))
                .replace("{moneda}", sym);
    }

    private BigDecimal guideRemainingTotal(Long guideId) {
        return installments.findByGuide_IdOrderBySeqAsc(guideId).stream()
                .map(this::remaining)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String formatMoney(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
