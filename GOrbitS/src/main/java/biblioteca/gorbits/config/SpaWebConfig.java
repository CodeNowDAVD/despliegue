package biblioteca.gorbits.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sirve el bundle de Angular bajo classpath:/static y devuelve {@code index.html} para rutas del
 * cliente (p. ej. {@code /admin/...}) que no correspondan a un fichero real.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(
                        new PathResourceResolver() {
                            @Override
                            protected Resource getResource(String resourcePath, Resource location)
                                    throws IOException {
                                Resource resource = super.getResource(resourcePath, location);
                                if (resource != null) {
                                    return resource;
                                }
                                if (resourcePath.startsWith("api/")) {
                                    return null;
                                }
                                return super.getResource("index.html", location);
                            }
                        });
    }
}
