package biblioteca.gorbits.config;

import biblioteca.gorbits.admin.AdminUsersService;
import biblioteca.gorbits.admin.dto.AdminCreateProviderRequest;
import biblioteca.gorbits.billing.BillingService;
import biblioteca.gorbits.billing.dto.CustomInstallmentItem;
import biblioteca.gorbits.billing.dto.CustomInstallmentPlanRequest;
import biblioteca.gorbits.billing.dto.RegisterPaymentRequest;
import biblioteca.gorbits.billing.dto.RescheduleInstallmentRequest;
import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import biblioteca.gorbits.commercial.dto.RegisterClientReturnRequest;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.dto.CreateLibraryPaymentRequest;
import biblioteca.gorbits.inventory.dto.CreateLibrarySupplyInvoiceRequest;
import biblioteca.gorbits.inventory.dto.LibrarySupplyLineItemRequest;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Datos demo completos: 5 embajadores para admin y 5 clientes del proveedor principal
 * cubriendo guías ACTIVA / CERRADA / DEVUELTA, cuotas, cobranza vencida y paquete.
 */
@Component
@Profile("h2")
@Order(500)
public class SeedDemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDemoDataInitializer.class);

    private static final String MARKER_USERNAME = "embajador01";

    private final UserAccountRepository users;
    private final AdminUsersService adminUsers;
    private final BookRepository books;
    private final CampaignRepository campaigns;
    private final SalesZoneRepository zones;
    private final CommercialService commercial;
    private final BillingService billing;
    private final InventoryService inventory;
    private final ProviderProfileRepository providerProfiles;

    public SeedDemoDataInitializer(
            UserAccountRepository users,
            AdminUsersService adminUsers,
            BookRepository books,
            CampaignRepository campaigns,
            SalesZoneRepository zones,
            ProviderProfileRepository providerProfiles,
            CommercialService commercial,
            BillingService billing,
            InventoryService inventory) {
        this.users = users;
        this.adminUsers = adminUsers;
        this.books = books;
        this.campaigns = campaigns;
        this.zones = zones;
        this.providerProfiles = providerProfiles;
        this.commercial = commercial;
        this.billing = billing;
        this.inventory = inventory;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername(MARKER_USERNAME).isPresent()) {
            return;
        }
        var proveedorOpt = users.findByUsername("proveedor");
        if (proveedorOpt.isEmpty()) {
            log.warn("Seed demo omitido: falta usuario «proveedor». Ejecutá de nuevo tras SeedUsers o borrá data/.");
            return;
        }
        UserAccount proveedor = proveedorOpt.get();
        List<Campaign> campaignRows = campaigns.findAllByOrderByStartsOnDesc();
        if (campaignRows.isEmpty()) {
            log.warn("Seed demo omitido: no hay campañas. Borrá GOrbitS/data/ y reiniciá.");
            return;
        }
        Campaign campaign = campaignRows.getFirst();
        List<SalesZone> zoneList = zones.findAll();
        if (zoneList.isEmpty()) {
            log.warn("Seed demo omitido: no hay zonas. Borrá GOrbitS/data/ y reiniciá.");
            return;
        }

        var bibliaOpt = findBook("El maravilloso mundo de la Biblia");
        var bebidasOpt = findBook("Bebidas saludables nutritivas y deliciosas");
        var vivirOpt = findBook("Vivir con Esperanza");
        var educacionOpt = findBook("Educación en el hogar");
        if (bibliaOpt.isEmpty() || bebidasOpt.isEmpty() || vivirOpt.isEmpty() || educacionOpt.isEmpty()) {
            log.warn(
                    "Seed demo omitido: catálogo incompleto. Borrá la carpeta GOrbitS/data/ y volvé a arrancar para regenerar H2.");
            return;
        }
        Book biblia = bibliaOpt.get();
        Book bebidas = bebidasOpt.get();
        Book vivirConEsperanza = vivirOpt.get();
        Book educacion = educacionOpt.get();

        seedMainProviderStock(proveedor, campaign, biblia, bebidas, vivirConEsperanza, educacion);
        seedProveedorClients(proveedor, campaign, biblia, bebidas, educacion);
        seedEmbajadores(campaign, zoneList, biblia);

        log.info(
                "Datos demo cargados: 5 embajadores ({} / embajador123) y 5 clientes del proveedor con flujos completos.",
                MARKER_USERNAME);
    }

    private void seedMainProviderStock(
            UserAccount proveedor,
            Campaign campaign,
            Book biblia,
            Book bebidas,
            Book vivirConEsperanza,
            Book educacion) {
        inventory.registerLibrarySupplyInvoice(
                proveedor,
                new CreateLibrarySupplyInvoiceRequest(
                        "FAC-DEMO-MAIN",
                        LocalDate.of(2026, 2, 1),
                        "Stock demo proveedor principal",
                        List.of(
                                new LibrarySupplyLineItemRequest(biblia.getId(), 120, null),
                                new LibrarySupplyLineItemRequest(bebidas.getId(), 60, null),
                                new LibrarySupplyLineItemRequest(vivirConEsperanza.getId(), 60, null),
                                new LibrarySupplyLineItemRequest(educacion.getId(), 40, null)),
                        null));
        inventory.registerLibraryPayment(
                proveedor,
                new CreateLibraryPaymentRequest(
                        new BigDecimal("500.00"),
                        LocalDate.of(2026, 2, 15),
                        "Depósito parcial demo",
                        campaign.getId()));
    }

    private void seedProveedorClients(
            UserAccount proveedor, Campaign campaign, Book biblia, Book bebidas, Book educacion) {
        long campId = campaign.getId();

        var ana = commercial.createClient(
                proveedor, new ClientRequest("Ana López", "999111001", "ana@demo.ejemplo", "Zona norte"));
        long guideActiva = commercial
                .createGuide(
                        proveedor,
                        new CreateGuideRequest(
                                campId,
                                ana.id(),
                                "000101",
                                LocalDate.of(2026, 3, 1),
                                GuideStatus.ACTIVA,
                                "Contrato activo con cuotas",
                                null,
                                List.of(new GuideLineRequest(biblia.getId(), 2, biblia.getPrice()))))
                .id();
        var planAna = billing.createCustomPlan(
                proveedor,
                guideActiva,
                new CustomInstallmentPlanRequest(List.of(
                        new CustomInstallmentItem(LocalDate.of(2026, 4, 1), new BigDecimal("20.00")),
                        new CustomInstallmentItem(LocalDate.of(2026, 5, 1), new BigDecimal("16.00")))));
        billing.registerPayment(
                proveedor,
                planAna.get(0).id(),
                new RegisterPaymentRequest(new BigDecimal("20.00"), LocalDate.of(2026, 4, 1), "Pago demo"));

        var carlos = commercial.createClient(
                proveedor, new ClientRequest("Carlos Ruiz", "999111002", null, null));
        long guideCerrada = commercial
                .createGuide(
                        proveedor,
                        new CreateGuideRequest(
                                campId,
                                carlos.id(),
                                "000102",
                                LocalDate.of(2026, 2, 10),
                                GuideStatus.CERRADA,
                                "Contrato cerrado",
                                null,
                                List.of(new GuideLineRequest(educacion.getId(), 1, educacion.getPrice()))))
                .id();
        var planCarlos = billing.createCustomPlan(
                proveedor,
                guideCerrada,
                new CustomInstallmentPlanRequest(
                        List.of(new CustomInstallmentItem(LocalDate.of(2026, 2, 28), educacion.getPrice()))));
        billing.registerPayment(
                proveedor,
                planCarlos.get(0).id(),
                new RegisterPaymentRequest(educacion.getPrice(), LocalDate.of(2026, 2, 28), null));

        var rosa = commercial.createClient(
                proveedor, new ClientRequest("Rosa Méndez", "999111003", null, "Devolución visible"));
        long guideDevuelta = commercial
                .createGuide(
                        proveedor,
                        new CreateGuideRequest(
                                campId,
                                rosa.id(),
                                "000103",
                                LocalDate.of(2026, 3, 5),
                                GuideStatus.ACTIVA,
                                null,
                                null,
                                List.of(new GuideLineRequest(biblia.getId(), 2, new BigDecimal("11.00")))))
                .id();
        billing.createCustomPlan(
                proveedor,
                guideDevuelta,
                new CustomInstallmentPlanRequest(
                        List.of(new CustomInstallmentItem(LocalDate.of(2026, 3, 20), new BigDecimal("22.00")))));
        commercial.registerClientReturn(
                proveedor,
                guideDevuelta,
                new RegisterClientReturnRequest(
                        "Cliente devolvió material completo", null, true, false));

        var pedro = commercial.createClient(
                proveedor, new ClientRequest("Pedro Vargas", "999111004", null, "Cobranza vencida"));
        long guideVencida = commercial
                .createGuide(
                        proveedor,
                        new CreateGuideRequest(
                                campId,
                                pedro.id(),
                                "000104",
                                LocalDate.of(2026, 1, 5),
                                GuideStatus.ACTIVA,
                                null,
                                null,
                                List.of(new GuideLineRequest(biblia.getId(), 1, biblia.getPrice()))))
                .id();
        var planPedro = billing.createCustomPlan(
                proveedor,
                guideVencida,
                new CustomInstallmentPlanRequest(
                        List.of(new CustomInstallmentItem(LocalDate.of(2026, 1, 1), biblia.getPrice()))));
        billing.reschedule(
                proveedor,
                planPedro.get(0).id(),
                new RescheduleInstallmentRequest(LocalDate.of(2026, 1, 15)));

        var lucia = commercial.createClient(
                proveedor, new ClientRequest("Lucía Paz", "999111005", "lucia@demo.ejemplo", null));
        long guidePaquete = commercial
                .createGuide(
                        proveedor,
                        new CreateGuideRequest(
                                campId,
                                lucia.id(),
                                "000105",
                                LocalDate.of(2026, 3, 12),
                                GuideStatus.ACTIVA,
                                "Paquete con libro incluido",
                                null,
                                List.of(new GuideLineRequest(bebidas.getId(), 1, bebidas.getPrice()))))
                .id();
        BigDecimal totalPaquete = commercial.getGuide(proveedor, guidePaquete).totalAmount();
        billing.createCustomPlan(
                proveedor,
                guidePaquete,
                new CustomInstallmentPlanRequest(
                        List.of(new CustomInstallmentItem(LocalDate.of(2026, 6, 1), totalPaquete))));
    }

    private void seedEmbajadores(Campaign campaign, List<SalesZone> zoneList, Book biblia) {
        record Emb(
                String user,
                String first,
                String last,
                String dni,
                String phone,
                String email,
                String career,
                int zoneIdx,
                String contract) {}

        List<Emb> specs = List.of(
                new Emb("embajador01", "Juan", "Pérez", "20100001", "70010001", "juan.perez@demo.ejemplo", "Teología", 0, "000501"),
                new Emb("embajador02", "Laura", "Soto", "20100002", "70010002", "laura.soto@demo.ejemplo", "Educación", 1, "000502"),
                new Emb("embajador03", "Miguel", "Ríos", "20100003", "70010003", "miguel.rios@demo.ejemplo", "Comunicación", 0, "000503"),
                new Emb("embajador04", "Carmen", "Díaz", "20100004", "70010004", "carmen.diaz@demo.ejemplo", "Psicología", 1, "000504"),
                new Emb("embajador05", "Diego", "Luna", "20100005", "70010005", "diego.luna@demo.ejemplo", "Administración", 0, "000505"));

        long campId = campaign.getId();
        int n = 0;
        for (Emb e : specs) {
            n++;
            adminUsers.createProvider(new AdminCreateProviderRequest(
                    e.user(),
                    "embajador123",
                    e.first(),
                    e.last(),
                    e.dni(),
                    e.phone(),
                    e.email(),
                    e.career()));
            UserAccount emb = users
                    .findByUsername(e.user())
                    .orElseThrow(() -> new IllegalStateException("Embajador no creado: " + e.user()));
            ProviderProfile profile = providerProfiles
                    .findWithZoneByUser_Id(emb.getId())
                    .orElseThrow(() -> new IllegalStateException("Perfil no creado: " + e.user()));
            profile.setZone(zoneList.get(e.zoneIdx() % zoneList.size()));
            providerProfiles.save(profile);

            inventory.registerLibrarySupplyInvoice(
                    emb,
                    new CreateLibrarySupplyInvoiceRequest(
                            "FAC-EMB-" + String.format("%02d", n),
                            LocalDate.of(2026, 2, 10),
                            "Stock demo embajador",
                            List.of(new LibrarySupplyLineItemRequest(biblia.getId(), 40, null)),
                            null));

            if (n <= 2) {
                inventory.registerLibraryPayment(
                        emb,
                        new CreateLibraryPaymentRequest(
                                new BigDecimal("200.00"),
                                LocalDate.of(2026, 2, 20),
                                "Depósito demo",
                                campId));
            }

            var client = commercial.createClient(
                    emb,
                    new ClientRequest(
                            e.first() + " " + e.last() + " (cliente)",
                            e.phone(),
                            e.email(),
                            "Cliente demo " + e.user()));
            commercial.createGuide(
                    emb,
                    new CreateGuideRequest(
                            campId,
                            client.id(),
                            e.contract(),
                            LocalDate.of(2026, 3, 15),
                            GuideStatus.ACTIVA,
                            "Venta demo embajador",
                            null,
                            List.of(new GuideLineRequest(biblia.getId(), 1, biblia.getPrice()))));
        }
    }

    private Optional<Book> findBook(String title) {
        return books.findAll().stream().filter(b -> title.equals(b.getTitle())).findFirst();
    }
}
