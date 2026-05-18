package biblioteca.gorbits.unit.commercial;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.ClientRepository;
import biblioteca.gorbits.commercial.CommercialService;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.SalesContractTagRepository;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.commercial.dto.ClientRequest;
import biblioteca.gorbits.commercial.dto.CreateGuideRequest;
import biblioteca.gorbits.commercial.dto.GuideLineRequest;
import biblioteca.gorbits.commercial.dto.PatchGuideStatusRequest;
import biblioteca.gorbits.commercial.dto.RegisterClientReturnRequest;
import biblioteca.gorbits.commercial.dto.UpdateProviderProfileRequest;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.ProviderStockService;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Cubre ramas orElseThrow de {@link CommercialService} (lambdas al 0 % en JaCoCo). */
@ExtendWith(MockitoExtension.class)
class CommercialServiceNotFoundUnitTest {

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
    void getClient_noEncontrado() {
        when(clients.findByIdAndOwner_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getClient(owner, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateClient_noEncontrado() {
        when(clients.findByIdAndOwner_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateClient(owner, 9L, new ClientRequest("X", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteClient_noEncontrado() {
        when(clients.findByIdAndOwner_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteClient(owner, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGuide_noEncontrada() {
        when(guides.findDetailedByIdAndOwner_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getGuide(owner, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProviderProfile_zonaNoEncontrada() {
        when(zones.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProviderProfile(owner, new UpdateProviderProfileRequest(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Zona");
    }

    @Test
    void updateProviderProfile_perfilNoEncontrado() {
        when(zones.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.zone(1L, "Norte")));
        when(profiles.findWithZoneByUser_Id(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProviderProfile(owner, new UpdateProviderProfileRequest(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Perfil");
    }

    @Test
    void createGuide_campanaNoEncontrada() {
        when(campaigns.findById(99L)).thenReturn(Optional.empty());
        var req = new CreateGuideRequest(
                99L,
                1L,
                "000001",
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new GuideLineRequest(1L, 1, BigDecimal.TEN)));
        assertThatThrownBy(() -> service.createGuide(owner, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaña");
    }

    @Test
    void createGuide_clienteNoEncontrado() {
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "C")));
        when(clients.findByIdAndOwner_Id(99L, 1L)).thenReturn(Optional.empty());
        var req = new CreateGuideRequest(
                1L,
                99L,
                "000002",
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new GuideLineRequest(1L, 1, BigDecimal.TEN)));
        assertThatThrownBy(() -> service.createGuide(owner, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cliente");
    }

    @Test
    void createGuide_libroNoEncontrado() {
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "C")));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(UnitTestFixtures.client(2L, owner, "Cli")));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000003")).thenReturn(false);
        when(books.findById(88L)).thenReturn(Optional.empty());
        var req = new CreateGuideRequest(
                1L,
                2L,
                "000003",
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new GuideLineRequest(88L, 1, BigDecimal.TEN)));
        assertThatThrownBy(() -> service.createGuide(owner, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Libro");
    }

    @Test
    void createGuide_etiquetaNoEncontrada() {
        when(campaigns.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.campaign(1L, "C")));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(UnitTestFixtures.client(2L, owner, "Cli")));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000004")).thenReturn(false);
        when(books.findById(10L))
                .thenReturn(Optional.of(UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "B", BigDecimal.TEN)));
        when(tags.findByIdAndOwner_Id(77L, 1L)).thenReturn(Optional.empty());
        var req = new CreateGuideRequest(
                1L,
                2L,
                "000004",
                LocalDate.now(),
                null,
                null,
                List.of(77L),
                List.of(new GuideLineRequest(10L, 1, BigDecimal.TEN)));
        assertThatThrownBy(() -> service.createGuide(owner, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Etiqueta");
    }

    @Test
    void createGuide_detalleTrasGuardarNoEncontrado() {
        var campaign = UnitTestFixtures.campaign(1L, "C");
        var client = UnitTestFixtures.client(2L, owner, "Cli");
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "B", BigDecimal.TEN);
        when(campaigns.findById(1L)).thenReturn(Optional.of(campaign));
        when(clients.findByIdAndOwner_Id(2L, 1L)).thenReturn(Optional.of(client));
        when(guides.existsByOwner_IdAndContractNumber(1L, "000005")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(guides.save(any(SalesGuide.class))).thenAnswer(inv -> {
            SalesGuide g = inv.getArgument(0);
            UnitTestFixtures.setId(g, 50L);
            return g;
        });
        when(guides.findDetailedByIdAndOwner_Id(50L, 1L)).thenReturn(Optional.empty());

        var req = new CreateGuideRequest(
                1L,
                2L,
                "000005",
                LocalDate.now(),
                null,
                null,
                null,
                List.of(new GuideLineRequest(10L, 1, BigDecimal.TEN)));
        assertThatThrownBy(() -> service.createGuide(owner, req)).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void patchGuideStatus_guiaNoEncontrada() {
        when(guides.findByIdAndOwner_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.patchGuideStatus(owner, 9L, new PatchGuideStatusRequest(GuideStatus.CERRADA)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void patchGuideStatus_detalleTrasGuardarNoEncontrado() {
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g));
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patchGuideStatus(owner, 10L, new PatchGuideStatusRequest(GuideStatus.CERRADA)))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void registerClientReturn_guiaNoEncontrada() {
        when(guides.findDetailedByIdAndOwner_Id(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registerClientReturn(
                        owner, 9L, new RegisterClientReturnRequest("motivo", null, false, false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerClientReturn_detalleTrasGuardarNoEncontrado() {
        var g = UnitTestFixtures.guide(
                10L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        when(guides.findDetailedByIdAndOwner_Id(10L, 1L)).thenReturn(Optional.of(g), Optional.empty());

        assertThatThrownBy(() -> service.registerClientReturn(
                        owner, 10L, new RegisterClientReturnRequest("motivo", null, false, false)))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
