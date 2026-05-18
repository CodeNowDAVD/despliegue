package biblioteca.gorbits.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityConfigUnitTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void passwordEncoder_esBcrypt() {
        String hash = config.passwordEncoder().encode("secreto");
        assertThat(hash).startsWith("$2a$");
        assertThat(config.passwordEncoder().matches("secreto", hash)).isTrue();
    }

    @Test
    void corsConfigurationSource_registraApi() {
        var source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        CorsConfiguration cors = source.getCorsConfiguration(request);
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedMethods()).contains(HttpMethod.GET.name());
        assertThat(cors.getExposedHeaders()).contains("Authorization");
    }

    @Test
    void isSpaOrStaticGet_getFueraDeApiYActuator_esPublico() {
        var request = new MockHttpServletRequest("GET", "/index.html");
        assertThat(SecurityConfig.isSpaOrStaticGet(request)).isTrue();
    }

    @Test
    void isSpaOrStaticGet_getApi_noEsPublico() {
        var request = new MockHttpServletRequest("GET", "/api/v1/me");
        assertThat(SecurityConfig.isSpaOrStaticGet(request)).isFalse();
    }

    @Test
    void isSpaOrStaticGet_getActuator_noEsPublico() {
        var request = new MockHttpServletRequest("GET", "/actuator/metrics");
        assertThat(SecurityConfig.isSpaOrStaticGet(request)).isFalse();
    }

    @Test
    void isSpaOrStaticGet_postNoGet_noEsPublico() {
        var request = new MockHttpServletRequest("POST", "/index.html");
        assertThat(SecurityConfig.isSpaOrStaticGet(request)).isFalse();
    }

    @Test
    void jsonAccessDeniedHandler_responde403Json() throws Exception {
        var handler = config.jsonAccessDeniedHandler();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denegado"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("No autorizado para esta operación");
    }

    @Test
    void jsonUnauthorizedEntryPoint_responde401Json() throws Exception {
        var entryPoint = config.jsonUnauthorizedEntryPoint();
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("test"));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("No autenticado");
    }

    @Test
    void authenticationManager_delegaEnConfiguracion() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(manager);

        assertThat(config.authenticationManager(authenticationConfiguration)).isSameAs(manager);
    }
}
