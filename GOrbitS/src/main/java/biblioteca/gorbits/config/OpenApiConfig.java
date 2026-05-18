package biblioteca.gorbits.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI gorbitsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GOrbitS API")
                        .version("0.0.1")
                        .description(
                                """
                                MVP: catálogo, zona comercial (clientes y guías de venta), inventario \
                                (almacén, campo, facturas de compra a librería), facturación por cuotas y panel admin. \
                                Autenticación: POST /api/v1/auth/login; usar accessToken en Authorization: Bearer …
                                """))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(
                                                "Obtener en /api/v1/auth/login; copiar el campo accessToken (sin 'Bearer ').")));
    }
}
