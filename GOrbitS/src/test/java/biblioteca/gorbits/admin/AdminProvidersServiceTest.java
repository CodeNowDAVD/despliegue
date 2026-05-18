package biblioteca.gorbits.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.admin.dto.AdminUpdateProviderRequest;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminProvidersServiceTest {

    @Mock
    ProviderProfileRepository profiles;

    @Mock
    UserAccountRepository users;

    @InjectMocks
    AdminProvidersService service;

    @Test
    void listProveedores_mapeaPerfiles() {
        var user = UnitTestFixtures.proveedor(3L);
        var profile = UnitTestFixtures.providerProfile(1L, user, UnitTestFixtures.zone(1L, "Sur"));
        when(profiles.findAllByUserRole(Role.PROVEEDOR)).thenReturn(List.of(profile));

        assertThat(service.listProveedores()).hasSize(1).first().satisfies(r -> assertThat(r.id()).isEqualTo(3L));
    }

    @Test
    void updateProveedor_rechazaSiNoEsProveedor() {
        var user = UnitTestFixtures.admin(1L);
        var profile = UnitTestFixtures.providerProfile(1L, user, null);
        when(profiles.findWithZoneByUser_Id(1L)).thenReturn(Optional.of(profile));

        var req = new AdminUpdateProviderRequest(
                "admin", "A", "B", "123", "1", "e@t.com", "Carrera");
        assertThatThrownBy(() -> service.updateProveedor(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("embajador");
    }

    @Test
    void updateProveedor_actualizaDatos() {
        var user = UnitTestFixtures.proveedor(5L);
        var profile = UnitTestFixtures.providerProfile(1L, user, null);
        when(profiles.findWithZoneByUser_Id(5L)).thenReturn(Optional.of(profile));
        when(profiles.existsByDniAndUser_IdNot("99999999", 5L)).thenReturn(false);
        when(users.existsByUsernameAndIdNot("emb05", 5L)).thenReturn(false);

        var req = new AdminUpdateProviderRequest(
                "emb05", "Nuevo", "Nombre", "99999999", "555", "n@t.com", "Ing");
        var result = service.updateProveedor(5L, req);

        assertThat(result.username()).isEqualTo("emb05");
        verify(users).save(user);
        verify(profiles).save(profile);
    }

    @Test
    void updateProveedor_noEncontrado() {
        when(profiles.findWithZoneByUser_Id(99L)).thenReturn(Optional.empty());
        var req = new AdminUpdateProviderRequest("x", "a", "b", "1", "2", "e@e.com", "c");
        assertThatThrownBy(() -> service.updateProveedor(99L, req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProveedor_rechazaDniDuplicado() {
        var user = UnitTestFixtures.proveedor(5L);
        var profile = UnitTestFixtures.providerProfile(1L, user, null);
        when(profiles.findWithZoneByUser_Id(5L)).thenReturn(Optional.of(profile));
        when(profiles.existsByDniAndUser_IdNot("11111111", 5L)).thenReturn(true);

        var req = new AdminUpdateProviderRequest("emb05", "A", "B", "11111111", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.updateProveedor(5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DNI");
    }

    @Test
    void updateProveedor_rechazaUsernameVacio() {
        var user = UnitTestFixtures.proveedor(5L);
        var profile = UnitTestFixtures.providerProfile(1L, user, null);
        when(profiles.findWithZoneByUser_Id(5L)).thenReturn(Optional.of(profile));
        when(profiles.existsByDniAndUser_IdNot("22222222", 5L)).thenReturn(false);

        var req = new AdminUpdateProviderRequest("   ", "A", "B", "22222222", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.updateProveedor(5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void updateProveedor_rechazaUsernameDuplicado() {
        var user = UnitTestFixtures.proveedor(5L);
        var profile = UnitTestFixtures.providerProfile(1L, user, null);
        when(profiles.findWithZoneByUser_Id(5L)).thenReturn(Optional.of(profile));
        when(profiles.existsByDniAndUser_IdNot("33333333", 5L)).thenReturn(false);
        when(users.existsByUsernameAndIdNot("otro", 5L)).thenReturn(true);

        var req = new AdminUpdateProviderRequest("otro", "A", "B", "33333333", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.updateProveedor(5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre de acceso");
    }
}
