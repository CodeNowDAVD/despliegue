package biblioteca.gorbits.unit.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.Client;
import biblioteca.gorbits.commercial.ClientRepository;
import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.SalesContractTag;
import biblioteca.gorbits.commercial.SalesContractTagRepository;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.CreateSalesContractTagRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.PatchGuideStatusRequest;
import biblioteca.gorbits.commercial.dto.RegisterClientReturnRequest;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.ProviderStockService;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommercialServiceExtendedUnitTest {

    @Mock
    SalesZoneRepository zones;

    @Mock
    CampaignRepository campaigns;

    @Mock
    ProviderProfileRepository profiles;

    @Mock
    ClientRepository clients;

    @Mock
    SalesGuideRepository guides;

    @Mock
    BookRepository books;

    @Mock
    SalesContractTagRepository tags;

    @Mock
    ProviderStockService providerStock;

    @Mock
    InventoryService inventory;

    @InjectMocks
    CommercialService service;

    private final UserAccount owner = UnitTestFixtures.proveedor(1L);

    @Test
    void createGuide_unitario_registraVenta() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(10L, cat, "Libro", new BigDecimal("25.00"));
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000888")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 99L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(99L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000888",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(new GuideLineRequest(10L, 2, new BigDecimal("25.00"))));

        var detail = service.createGuide(owner, req);

        assertThat(detail.contractNumber()).isEqualTo("000888");
        assertThat(detail.lines()).hasSize(1);
        verify(providerStock).ensureAvailable(eq(owner), any());
        verify(inventory).logContractSale(eq(owner), eq(99L), any(), any());
    }

    @Test
    void createGuide_conEstadoExplicito_usaStatusDelRequest() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(10L, cat, "Libro", new BigDecimal("25.00"));
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000889")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 106L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(106L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000889",
                LocalDate.of(2026, 3, 1),
                GuideStatus.CERRADA,
                null,
                null,
                List.of(new GuideLineRequest(10L, 1, new BigDecimal("25.00"))));

        assertThat(service.createGuide(owner, req).status()).isEqualTo(GuideStatus.CERRADA);
    }

    @Test
    void createGuide_paquete_agregaLineaComplemento() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota");
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000889")).thenReturn(false);
        when(books.findById(12L)).thenReturn(Optional.of(pack));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 100L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(100L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000889",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(new GuideLineRequest(12L, 3, new BigDecimal("50.00"))));

        var detail = service.createGuide(owner, req);

        assertThat(detail.lines()).hasSize(2);
    }

    @Test
    void createGuide_paquete_noDuplicaComplementoSiYaEstaEnGuia() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota");
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000890")).thenReturn(false);
        when(books.findById(11L)).thenReturn(Optional.of(companion));
        when(books.findById(12L)).thenReturn(Optional.of(pack));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 101L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(101L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000890",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(
                        new GuideLineRequest(11L, 1, new BigDecimal("0.01")),
                        new GuideLineRequest(12L, 1, new BigDecimal("50.00"))));

        var detail = service.createGuide(owner, req);

        assertThat(detail.lines()).hasSize(2);
    }

    @Test
    void createGuide_tagIdsVacios_noAsignaEtiquetas() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(10L, cat, "Libro", new BigDecimal("25.00"));
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000892")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 103L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(103L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000892",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                List.of(),
                List.of(new GuideLineRequest(10L, 1, new BigDecimal("25.00"))));

        assertThat(service.createGuide(owner, req).tags()).isEmpty();
    }

    @Test
    void createGuide_paqueteSinComplementoEnCatalogo_noAgregaLineaExtra() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var pack = new Book(cat, "Pack suelto", new BigDecimal("50.00"), BookType.PAQUETE, "nota");
        UnitTestFixtures.setId(pack, 12L);
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000893")).thenReturn(false);
        when(books.findById(12L)).thenReturn(Optional.of(pack));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 104L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(104L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000893",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(new GuideLineRequest(12L, 2, new BigDecimal("50.00"))));

        assertThat(service.createGuide(owner, req).lines()).hasSize(1);
    }

    @Test
    void createGuide_dosEtiquetas_resuelveAmbas() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(10L, cat, "Libro", new BigDecimal("25.00"));
        var tag1 = new SalesContractTag(owner, "VIP");
        UnitTestFixtures.setId(tag1, 5L);
        var tag2 = new SalesContractTag(owner, "Urgente");
        UnitTestFixtures.setId(tag2, 6L);
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000894")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(tags.findByIdAndOwner_Id(5L, 1L)).thenReturn(Optional.of(tag1));
        when(tags.findByIdAndOwner_Id(6L, 1L)).thenReturn(Optional.of(tag2));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 105L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(105L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000894",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                List.of(5L, 6L),
                List.of(new GuideLineRequest(10L, 1, new BigDecimal("25.00"))));

        assertThat(service.createGuide(owner, req).tags()).containsExactlyInAnyOrder("VIP", "Urgente");
    }

    @Test
    void createGuide_conEtiquetas_resuelveTags() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(10L, cat, "Libro", new BigDecimal("25.00"));
        var tag = new SalesContractTag(owner, "VIP");
        UnitTestFixtures.setId(tag, 5L);
        AtomicReference<SalesGuide> saved = new AtomicReference<>();

        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000891")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(tags.findByIdAndOwner_Id(5L, 1L)).thenReturn(Optional.of(tag));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 102L);
            saved.set(g);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(102L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000891",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                List.of(5L),
                List.of(new GuideLineRequest(10L, 1, new BigDecimal("25.00"))));

        var detail = service.createGuide(owner, req);

        assertThat(detail.tags()).containsExactly("VIP");
    }

    @Test
    void getGuide_devueltaSinMeta_noIncluyeClientReturn() {
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.DEVUELTA);
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));

        assertThat(service.getGuide(owner, 10L).clientReturn()).isNull();
    }

    @Test
    void patchGuideStatus_cerrada_ok() {
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        g.addLine(UnitTestFixtures.book(5L, UnitTestFixtures.category(1L, "C"), "B", BigDecimal.ONE), 1, BigDecimal.ONE);
        when(guides.findByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));

        var detail = service.patchGuideStatus(owner, 10L, new PatchGuideStatusRequest(GuideStatus.CERRADA));

        assertThat(detail.status()).isEqualTo(GuideStatus.CERRADA);
    }

    @Test
    void registerClientReturn_restauraStock() {
        var book = UnitTestFixtures.book(7L, UnitTestFixtures.category(1L, "C"), "B", new BigDecimal("10"));
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        g.addLine(book, 2, new BigDecimal("10"));
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));

        var req = new RegisterClientReturnRequest("Cliente no pagó", Instant.parse("2026-04-01T10:00:00Z"), true, false);
        var detail = service.registerClientReturn(owner, 10L, req);

        assertThat(detail.status()).isEqualTo(GuideStatus.DEVUELTA);
        verify(inventory).logContractClientReturn(eq(owner), eq(10L), any(), any());
    }

    @Test
    void listGuideReturns_excluyeOcultas() {
        var visible = UnitTestFixtures.guide(
                1L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "A"), GuideStatus.DEVUELTA);
        visible.setClientReturnMeta(Instant.now(), "motivo", false);
        var hidden = UnitTestFixtures.guide(
                2L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(2L, owner, "B"), GuideStatus.DEVUELTA);
        hidden.setClientReturnMeta(Instant.now(), "oculto", true);
        when(guides.findByOwner_IdAndStatusForReturns(1L, GuideStatus.DEVUELTA)).thenReturn(List.of(visible, hidden));

        assertThat(service.listGuideReturns(owner, false)).hasSize(1);
        assertThat(service.listGuideReturns(owner, true)).hasSize(2);
    }

    @Test
    void createSalesContractTag_ok() {
        when(tags.existsByOwner_IdAndNameIgnoreCase(1L, "Nuevo")).thenReturn(false);
        when(tags.save(any(SalesContractTag.class))).thenAnswer(inv -> {
            SalesContractTag t = inv.getArgument(0);
            UnitTestFixtures.setId(t, 8L);
            return t;
        });

        var res = service.createSalesContractTag(owner, new CreateSalesContractTagRequest("Nuevo"));
        assertThat(res.name()).isEqualTo("Nuevo");
        assertThat(res.id()).isEqualTo(8L);
    }

    @Test
    void listGuides_conEtiquetaYBusqueda() {
        var g = UnitTestFixtures.guide(
                1L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findByOwner_IdAndTag_IdAndContractNumberContainingIgnoreCaseOrderByCreatedAtDesc(1L, 5L, "000"))
                .thenReturn(List.of(g));

        assertThat(service.listGuides(owner, 5L, "000")).hasSize(1);
    }

    @Test
    void listGuides_sinEtiquetaConBusqueda() {
        var g = UnitTestFixtures.guide(
                1L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findByOwner_IdAndContractNumberContainingIgnoreCaseOrderByCreatedAtDesc(1L, "abc"))
                .thenReturn(List.of(g));

        assertThat(service.listGuides(owner, null, "abc")).hasSize(1);
    }

    @Test
    void listGuides_conEtiquetaSinBusqueda() {
        var g = UnitTestFixtures.guide(
                1L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findByOwner_IdAndTag_IdOrderByCreatedAtDesc(1L, 5L)).thenReturn(List.of(g));

        assertThat(service.listGuides(owner, 5L, null)).hasSize(1);
    }

    @Test
    void listGuides_sinEtiquetaNiBusqueda() {
        var g = UnitTestFixtures.guide(
                1L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findByOwner_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(g));

        assertThat(service.listGuides(owner, null, null)).hasSize(1);
    }

    @Test
    void listCampaigns_yClientesConBusqueda() {
        when(campaigns.findAllByOrderByStartsOnDesc())
                .thenReturn(List.of(UnitTestFixtures.campaign(1L, "Invierno")));
        when(clients.findByOwner_IdAndFullNameContainingIgnoreCaseOrderByFullNameAsc(1L, "ana"))
                .thenReturn(List.of(UnitTestFixtures.client(2L, owner, "Ana López")));

        assertThat(service.listCampaigns()).hasSize(1).first().satisfies(c -> assertThat(c.name()).isEqualTo("Invierno"));
        assertThat(service.listClients(owner, "  ana ")).hasSize(1);
    }

    @Test
    void listClients_queryVacio_listaTodos() {
        when(clients.findByOwner_IdOrderByFullNameAsc(1L))
                .thenReturn(List.of(UnitTestFixtures.client(2L, owner, "Ana")));

        assertThat(service.listClients(owner, "   ")).hasSize(1);
    }

    @Test
    void createClient_camposOpcionalesVacios_guardaNull() {
        when(clients.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            UnitTestFixtures.setId(c, 6L);
            return c;
        });

        var res = service.createClient(owner, new ClientRequest("Nombre", "   ", null, "  "));

        assertThat(res.phone()).isNull();
        assertThat(res.addressNote()).isNull();
    }

    @Test
    void deleteClient_sinGuias_elimina() {
        var c = UnitTestFixtures.client(3L, owner, "X");
        when(clients.findByIdAndOwner_Id(3L, 1L)).thenReturn(Optional.of(c));
        when(guides.existsByClient_Id(3L)).thenReturn(false);

        service.deleteClient(owner, 3L);

        verify(clients).delete(c);
    }
}
