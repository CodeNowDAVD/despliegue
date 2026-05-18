package biblioteca.gorbits.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Contenedor MariaDB compartido para pruebas de repositorio (@DataJpaTest, perfil {@code tc}).
 * Arranque diferido: solo inicia al registrar propiedades (no en carga estática de la clase).
 * Requiere Docker; ejecutar con {@code -DexcludedGroups=} para incluir tests {@code @Tag("testcontainers")}.
 */
public final class MariaDBTestContainer {

    private static final DockerImageName MARIADB_IMAGE = DockerImageName.parse("mariadb:11.4");

    private static final MariaDBContainer<?> CONTAINER = new MariaDBContainer<>(MARIADB_IMAGE)
            .withDatabaseName("gorbits_tc")
            .withUsername("tc_user")
            .withPassword("tc_pass");

    private MariaDBTestContainer() {}

    public static MariaDBContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    public static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> container().getJdbcUrl());
        registry.add("spring.datasource.username", () -> container().getUsername());
        registry.add("spring.datasource.password", () -> container().getPassword());
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }
}
