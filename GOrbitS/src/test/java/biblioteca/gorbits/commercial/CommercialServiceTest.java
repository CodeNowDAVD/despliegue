package biblioteca.gorbits.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.CreateSalesContractTagRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommercialServiceTest {

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
    void createClient_guardaYDevuelve() {
        when(clients.save(any(Client.class))).thenAnswer(inv -> {
            Client c = inv.getArgument(0);
            UnitTestFixtures.setId(c, 5L);
            return c;
        });

        var res = service.createClient(owner, new ClientRequest("  Juan Pérez  ", "999", null, null));
        assertThat(res.fullName()).isEqualTo("Juan Pérez");
        assertThat(res.id()).isEqualTo(5L);
    }

    @Test
    void deleteClient_conGuias_lanza() {
        var c = UnitTestFixtures.client(3L, owner, "X");
        when(clients.findByIdAndOwner_Id(3L, 1L)).thenReturn(Optional.of(c));
        when(guides.existsByClient_Id(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteClient(owner, 3L)).isInstanceOf(IllegalStateException.class);
        verify(clients, never()).delete(any());
    }

    @Test
    void createSalesContractTag_rechazaDuplicado() {
        when(tags.existsByOwner_IdAndNameIgnoreCase(1L, "VIP")).thenReturn(true);
        assertThatThrownBy(() -> service.createSalesContractTag(owner, new CreateSalesContractTagRequest("VIP")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerClientReturn_requiereMotivo() {
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));

        var req = new RegisterClientReturnRequest("   ", null, false, false);
        assertThatThrownBy(() -> service.registerClientReturn(owner, 10L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void patchGuideStatus_rechazaDevueltaDirecta() {
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));

        assertThatThrownBy(() -> service.patchGuideStatus(owner, 10L, new PatchGuideStatusRequest(GuideStatus.DEVUELTA)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getProviderProfile_noEncontrado() {
        when(profiles.findWithZoneByUser_Id(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProviderProfile(owner)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listZones_mapea() {
        when(zones.findAllByOrderByNameAsc()).thenReturn(List.of(UnitTestFixtures.zone(1L, "Norte")));
        assertThat(service.listZones()).hasSize(1).first().satisfies(z -> assertThat(z.name()).isEqualTo("Norte"));
    }

    @Test
    void createGuide_rechazaContratoDuplicado() {
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "C")));
        when(clients.findByIdAndOwner_Id(2L, 1L))
                .thenReturn(Optional.of(UnitTestFixtures.client(2L, owner, "Cli")));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000777")).thenReturn(true);

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000777",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(new GuideLineRequest(10L, 1, BigDecimal.ONE)));

        assertThatThrownBy(() -> service.createGuide(owner, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contrato");
    }

    @Test
    void createGuide_libroNoEncontrado() {
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "C")));
        when(clients.findByIdAndOwner_Id(2L, 1L))
                .thenReturn(Optional.of(UnitTestFixtures.client(2L, owner, "Cli")));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000778")).thenReturn(false);
        when(books.findById(99L)).thenReturn(Optional.empty());

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000778",
                LocalDate.of(2026, 3, 1),
                null,
                null,
                null,
                List.of(new GuideLineRequest(99L, 1, BigDecimal.ONE)));

        assertThatThrownBy(() -> service.createGuide(owner, req)).isInstanceOf(ResourceNotFoundException.class);
    }
}
