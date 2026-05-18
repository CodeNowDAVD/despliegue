package biblioteca.gorbits.unit.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.config.SeedCommercialInitializer;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccountRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeedCommercialInitializerUnitTest {

    @Mock
    SalesZoneRepository zones;

    @Mock
    CampaignRepository campaigns;

    @Mock
    ProviderProfileRepository profiles;

    @Mock
    UserAccountRepository users;

    @InjectMocks
    SeedCommercialInitializer initializer;

    @Test
    void run_datosCompletos_noDuplica() throws Exception {
        var campoA = UnitTestFixtures.zone(1L, "Campo A");
        var campoB = UnitTestFixtures.zone(2L, "Campo B");
        var campaign = new Campaign("Campaña 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        var proveedor = UnitTestFixtures.proveedor(3L);
        var profile = UnitTestFixtures.providerProfile(10L, proveedor, campoA);

        when(zones.findAll()).thenReturn(List.of(campoA, campoB));
        when(campaigns.findAllByOrderByStartsOnDesc()).thenReturn(List.of(campaign));
        when(users.findByUsername("proveedor")).thenReturn(Optional.of(proveedor));
        when(profiles.findWithZoneByUser_Id(3L)).thenReturn(Optional.of(profile));

        initializer.run();

        verify(campaigns, never()).save(any());
        verify(profiles, never()).save(any());
    }

    @Test
    void run_sinCampañaNiPerfil_crea() throws Exception {
        var proveedor = UnitTestFixtures.proveedor(3L);

        when(zones.findAll()).thenReturn(List.of());
        when(zones.save(any(SalesZone.class))).thenAnswer(inv -> {
            SalesZone z = inv.getArgument(0);
            UnitTestFixtures.setId(z, 1L);
            return z;
        });
        when(campaigns.findAllByOrderByStartsOnDesc()).thenReturn(List.of());
        when(users.findByUsername("proveedor")).thenReturn(Optional.of(proveedor));
        when(profiles.findWithZoneByUser_Id(3L)).thenReturn(Optional.empty());

        initializer.run();

        verify(campaigns).save(any(Campaign.class));
        verify(profiles).save(any(ProviderProfile.class));
    }

    @Test
    void run_zonaExistentePorNombre_noGuardaDuplicada() throws Exception {
        var campoA = UnitTestFixtures.zone(1L, "Campo A");
        when(zones.findAll()).thenReturn(List.of(campoA));
        when(campaigns.findAllByOrderByStartsOnDesc())
                .thenReturn(List.of(new Campaign("Campaña 2026", LocalDate.now(), LocalDate.now().plusMonths(1))));
        when(users.findByUsername("proveedor")).thenReturn(Optional.empty());

        initializer.run();

        verify(zones, never()).save(campoA);
    }
}
