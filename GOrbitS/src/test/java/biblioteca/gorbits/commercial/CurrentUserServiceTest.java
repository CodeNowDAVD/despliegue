package biblioteca.gorbits.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    UserAccountRepository users;

    @InjectMocks
    CurrentUserService service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUser_resuelvePorNombreDeSesion() {
        var account = UnitTestFixtures.proveedor(5L);
        when(users.findByUsername("proveedor")).thenReturn(java.util.Optional.of(account));
        SecurityContextHolder.getContext()
                .setAuthentication(authenticated("proveedor", "ROLE_PROVEEDOR"));

        assertThat(service.currentUser()).isSameAs(account);
    }

    @Test
    void requireProveedor_rechazaAdmin() {
        var admin = UnitTestFixtures.admin(1L);
        when(users.findByUsername("admin")).thenReturn(java.util.Optional.of(admin));
        SecurityContextHolder.getContext().setAuthentication(authenticated("admin", "ROLE_ADMIN"));

        assertThatThrownBy(() -> service.requireProveedor())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN);
    }

    @Test
    void currentUser_sinAutenticacion_lanza401() {
        assertThatThrownBy(() -> service.currentUser()).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void currentUser_sesionNoAutenticada_lanza401() {
        var token = new UsernamePasswordAuthenticationToken("proveedor", "n/a", List.of());
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThatThrownBy(() -> service.currentUser())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void currentUser_usuarioNoEncontradoEnBd_lanza401() {
        when(users.findByUsername("fantasma")).thenReturn(java.util.Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(authenticated("fantasma", "ROLE_PROVEEDOR"));

        assertThatThrownBy(() -> service.currentUser())
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requireProveedor_ok() {
        var account = UnitTestFixtures.proveedor(5L);
        when(users.findByUsername("proveedor")).thenReturn(java.util.Optional.of(account));
        SecurityContextHolder.getContext().setAuthentication(authenticated("proveedor", "ROLE_PROVEEDOR"));

        assertThat(service.requireProveedor()).isSameAs(account);
    }

    private static UsernamePasswordAuthenticationToken authenticated(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, "n/a", List.of(new SimpleGrantedAuthority(role)));
    }
}
