package biblioteca.gorbits.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void gorbitsOpenApi_defineInfoYJwt() {
        var api = new OpenApiConfig().gorbitsOpenApi();
        assertThat(api.getInfo().getTitle()).isEqualTo("GOrbitS API");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearer-jwt");
    }
}
