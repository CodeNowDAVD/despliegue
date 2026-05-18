package biblioteca.gorbits.api;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.catalog.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
    }

    @Test
    void notFound() {
        var r = handler.notFound(new ResourceNotFoundException("no existe"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("error", "no existe");
    }

    @Test
    void badRequest() {
        var r = handler.badRequest(new IllegalArgumentException("dato inválido"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void conflict() {
        var r = handler.conflict(new IllegalStateException("estado inválido"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void forbidden() {
        var r = handler.forbidden(new AccessDeniedException("denegado"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void badCredentials() {
        var r = handler.badCredentials(new BadCredentialsException("x"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

}
