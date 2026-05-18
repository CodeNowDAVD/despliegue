package biblioteca.gorbits.commercial;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import org.junit.jupiter.api.Test;

class EmbassadorProfileMapperTest {

    @Test
    void toResponse_sinZona_devuelveNullEnCamposDeZona() {
        UserAccount user = UnitTestFixtures.proveedor(10L);
        ProviderProfile profile = UnitTestFixtures.providerProfile(1L, user, null);
        profile.setPersonalData("Ana", "López", "12345678", "999", "a@test.com", "Teología");

        var response = EmbassadorProfileMapper.toResponse(profile, user);

        assertThat(response.zoneId()).isNull();
        assertThat(response.zoneName()).isNull();
    }

    @Test
    void toResponse_incluyeZonaCuandoExiste() {
        UserAccount user = UnitTestFixtures.proveedor(10L);
        SalesZone zone = UnitTestFixtures.zone(5L, "Norte");
        ProviderProfile profile = UnitTestFixtures.providerProfile(1L, user, zone);
        profile.setPersonalData("Ana", "López", "12345678", "999", "a@test.com", "Teología");

        var response = EmbassadorProfileMapper.toResponse(profile, user);

        assertThat(response.username()).isEqualTo("proveedor");
        assertThat(response.zoneId()).isEqualTo(5L);
        assertThat(response.zoneName()).isEqualTo("Norte");
        assertThat(response.dni()).isEqualTo("12345678");
    }

    @Test
    void toAdminListItem_mapeaUsuarioYPerfil() {
        UserAccount user = UnitTestFixtures.proveedor(20L);
        ProviderProfile profile = UnitTestFixtures.providerProfile(2L, user, null);
        profile.setPersonalData("Luis", "García", "87654321", null, null, null);

        var item = EmbassadorProfileMapper.toAdminListItem(profile);

        assertThat(item.id()).isEqualTo(20L);
        assertThat(item.username()).isEqualTo("proveedor");
        assertThat(item.zoneId()).isNull();
    }
}
