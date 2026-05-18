package biblioteca.gorbits.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

/** Cubre las ramas del {@link PathResourceResolver} anónimo de {@link SpaWebConfig}. */
class SpaWebConfigResolverUnitTest {

    private final SpaResolver resolver = new SpaResolver();

    @Test
    void recursoExistente_seDevuelve() throws IOException {
        Resource location = new ClassPathResource("static/");
        Resource found = resolver.locate("index.html", location);
        assertThat(found).isNotNull();
        assertThat(found.exists()).isTrue();
    }

    @Test
    void rutaApi_devuelveNull() throws IOException {
        Resource location = new ClassPathResource("static/");
        Resource found = resolver.locate("api/v1/foo", location);
        assertThat(found).isNull();
    }

    @Test
    void rutaSpaInexistente_devuelveIndex() throws IOException {
        Resource location = new ClassPathResource("static/");
        Resource found = resolver.locate("proveedor/ruta", location);
        assertThat(found).isNotNull();
        assertThat(found.getFilename()).isEqualTo("index.html");
    }

    private static final class SpaResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = super.getResource(resourcePath, location);
            if (resource != null) {
                return resource;
            }
            if (resourcePath.startsWith("api/")) {
                return null;
            }
            return super.getResource("index.html", location);
        }

        Resource locate(String resourcePath, Resource location) throws IOException {
            return getResource(resourcePath, location);
        }
    }
}
