package biblioteca.gorbits.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.admin.dto.AdminCreateProviderRequest;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUsersServiceTest {

    @Mock
    UserAccountRepository users;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    ProviderProfileRepository providerProfiles;

    @Mock
    SalesZoneRepository zones;

    @InjectMocks
    AdminUsersService service;

    @Test
    void resetPassword_validaLongitudMinima() {
        var u = UnitTestFixtures.admin(1L);
        when(users.findById(1L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.resetPassword(1L, "corta"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void resetPassword_actualizaHash() {
        var u = UnitTestFixtures.admin(1L);
        when(users.findById(1L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("nueva1234")).thenReturn("encoded");

        var summary = service.resetPassword(1L, "nueva1234");

        assertThat(summary.username()).isEqualTo("admin");
        verify(users).save(u);
        assertThat(u.getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void setEnabled_impideDeshabilitarAlPropioAdmin() {
        assertThatThrownBy(() -> service.setEnabled(1L, false, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("su propia cuenta");
    }

    @Test
    void updateRole_impideQuitarUltimoAdmin() {
        var u = UnitTestFixtures.admin(1L);
        when(users.findById(2L)).thenReturn(Optional.of(u));
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.updateRole(2L, Role.PROVEEDOR, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un administrador");
    }

    @Test
    void updateRole_cambioAAdmin_noEvaluaConteoCuandoNuevoRolNoEsProveedor() {
        var u = spy(UnitTestFixtures.admin(4L));
        when(users.findById(4L)).thenReturn(Optional.of(u));
        when(u.getRole()).thenReturn(Role.PROVEEDOR, Role.ADMIN);

        var summary = service.updateRole(4L, Role.ADMIN, 1L);

        assertThat(summary.role()).isEqualTo(Role.ADMIN);
        verify(users).save(u);
        verify(users, never()).countByRole(Role.ADMIN);
    }

    @Test
    void createProvider_creaUsuarioYPerfil() {
        when(users.existsByUsername("nuevo")).thenReturn(false);
        when(providerProfiles.existsByDni("11111111")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hash");
        when(zones.findFirstByOrderByIdAsc()).thenReturn(Optional.of(UnitTestFixtures.zone(1L, "Centro")));
        when(users.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            UnitTestFixtures.setId(saved, 50L);
            return saved;
        });

        var req = new AdminCreateProviderRequest(
                "nuevo", "password1", "Ana", "Pérez", "11111111", "999", "a@test.com", "Teología");
        var summary = service.createProvider(req);

        assertThat(summary.username()).isEqualTo("nuevo");
        assertThat(summary.role()).isEqualTo(Role.PROVEEDOR);
        verify(providerProfiles).save(any());
    }

    @Test
    void listUsers_ordenados() {
        when(users.findAllByOrderByUsernameAsc())
                .thenReturn(List.of(UnitTestFixtures.admin(1L), UnitTestFixtures.proveedor(2L)));
        assertThat(service.listUsers()).hasSize(2);
    }

    @Test
    void resetPassword_usuarioInexistente() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword(99L, "password123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setEnabled_habilitaOtroUsuario() {
        var u = UnitTestFixtures.proveedor(2L);
        when(users.findById(2L)).thenReturn(Optional.of(u));

        var summary = service.setEnabled(2L, false, 1L);

        assertThat(summary.enabled()).isFalse();
        verify(users).save(u);
    }

    @Test
    void setEnabled_adminPuedeHabilitarse() {
        var u = UnitTestFixtures.admin(1L);
        u.setEnabled(false);
        when(users.findById(1L)).thenReturn(Optional.of(u));

        var summary = service.setEnabled(1L, true, 1L);

        assertThat(summary.enabled()).isTrue();
        verify(users).save(u);
    }

    @Test
    void setEnabled_usuarioInexistente() {
        when(users.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setEnabled(5L, true, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRole_noPermiteCambiarPropioRol() {
        assertThatThrownBy(() -> service.updateRole(1L, Role.PROVEEDOR, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propio rol");
    }

    @Test
    void updateRole_mismoRol_sinGuardar() {
        var u = UnitTestFixtures.proveedor(3L);
        when(users.findById(3L)).thenReturn(Optional.of(u));

        var summary = service.updateRole(3L, Role.PROVEEDOR, 1L);

        assertThat(summary.role()).isEqualTo(Role.PROVEEDOR);
        verify(users, never()).save(any());
    }

    @Test
    void updateRole_aProveedor_creaPerfilSiFalta() {
        var u = UnitTestFixtures.admin(4L);
        when(users.findById(4L)).thenReturn(Optional.of(u));
        when(users.countByRole(Role.ADMIN)).thenReturn(2L);
        when(providerProfiles.existsByUser_Id(4L)).thenReturn(false);
        when(zones.findFirstByOrderByIdAsc()).thenReturn(Optional.of(UnitTestFixtures.zone(1L, "Sur")));

        var summary = service.updateRole(4L, Role.PROVEEDOR, 1L);

        assertThat(summary.role()).isEqualTo(Role.PROVEEDOR);
        verify(providerProfiles).save(any());
    }

    @Test
    void updateRole_aProveedor_perfilExistente_noCreaOtro() {
        var u = UnitTestFixtures.admin(4L);
        when(users.findById(4L)).thenReturn(Optional.of(u));
        when(users.countByRole(Role.ADMIN)).thenReturn(2L);
        when(providerProfiles.existsByUser_Id(4L)).thenReturn(true);

        var summary = service.updateRole(4L, Role.PROVEEDOR, 1L);

        assertThat(summary.role()).isEqualTo(Role.PROVEEDOR);
        verify(providerProfiles, never()).save(any());
    }

    @Test
    void updateRole_aProveedor_sinZonasEnEnsurePerfil_lanza() {
        var u = UnitTestFixtures.admin(4L);
        when(users.findById(4L)).thenReturn(Optional.of(u));
        when(users.countByRole(Role.ADMIN)).thenReturn(2L);
        when(providerProfiles.existsByUser_Id(4L)).thenReturn(false);
        when(zones.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole(4L, Role.PROVEEDOR, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zonas comerciales");
    }

    @Test
    void updateRole_aAdmin_desdeProveedor() {
        var u = UnitTestFixtures.proveedor(3L);
        when(users.findById(3L)).thenReturn(Optional.of(u));

        var summary = service.updateRole(3L, Role.ADMIN, 1L);

        assertThat(summary.role()).isEqualTo(Role.ADMIN);
        verify(providerProfiles, never()).save(any());
    }

    @Test
    void updateRole_usuarioInexistente() {
        when(users.findById(88L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRole(88L, Role.ADMIN, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createProvider_rechazaUsuarioDuplicado() {
        when(users.existsByUsername("dup")).thenReturn(true);
        var req = new AdminCreateProviderRequest(
                "dup", "password1", "A", "B", "22222222", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.createProvider(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void createProvider_rechazaDniDuplicado() {
        when(users.existsByUsername("nuevo2")).thenReturn(false);
        when(providerProfiles.existsByDni("33333333")).thenReturn(true);
        var req = new AdminCreateProviderRequest(
                "nuevo2", "password1", "A", "B", "33333333", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.createProvider(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DNI");
    }

    @Test
    void createProvider_rechazaUsuarioVacio() {
        var req = new AdminCreateProviderRequest(
                "   ", "password1", "A", "B", "44444444", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.createProvider(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void createProvider_sinZonas_lanza() {
        when(users.existsByUsername("sinzona")).thenReturn(false);
        when(providerProfiles.existsByDni("55555555")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(users.save(any(UserAccount.class))).thenAnswer(inv -> {
            UserAccount saved = inv.getArgument(0);
            UnitTestFixtures.setId(saved, 60L);
            return saved;
        });
        when(zones.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        var req = new AdminCreateProviderRequest(
                "sinzona", "password1", "A", "B", "55555555", "1", "e@t.com", "C");
        assertThatThrownBy(() -> service.createProvider(req)).isInstanceOf(IllegalStateException.class);
    }
}
