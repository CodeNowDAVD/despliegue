package biblioteca.gorbits.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.config.MariaDBTestContainer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de capa repository con base real MariaDB en Docker (Testcontainers).
 * Evidencia CRUD sobre {@link BookCategory} sin MockMvc ni H2 en memoria del perfil test.
 */
@DataJpaTest
@Testcontainers
@Tag("testcontainers")
@ActiveProfiles("tc")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookCategoryRepositoryTest {

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        MariaDBTestContainer.registerDataSourceProperties(registry);
    }

    @Autowired
    BookCategoryRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void guardarYBuscarPorId() {
        BookCategory saved = repository.save(new BookCategory("Teología"));
        assertThat(saved.getId()).isNotNull();

        BookCategory found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Teología");
    }

    @Test
    void listarOrdenadoPorId() {
        repository.save(new BookCategory("Historia"));
        repository.save(new BookCategory("Literatura"));

        List<BookCategory> all = repository.findAll();
        assertThat(all).hasSize(2);
    }

    @Test
    void actualizarNombre() {
        BookCategory cat = repository.save(new BookCategory("Ciencias"));
        cat.setName("Ciencias Sociales");
        repository.save(cat);

        assertThat(repository.findById(cat.getId()).orElseThrow().getName()).isEqualTo("Ciencias Sociales");
    }

    @Test
    void eliminarPorId() {
        BookCategory cat = repository.save(new BookCategory("Temporal"));
        Long id = cat.getId();
        repository.deleteById(id);
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void existsByNameIgnoreCase() {
        repository.save(new BookCategory("Música"));
        assertThat(repository.existsByNameIgnoreCase("música")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("Arte")).isFalse();
    }
}
