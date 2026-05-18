package biblioteca.gorbits.unit.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookCategory;
import biblioteca.gorbits.catalog.BookCategoryRepository;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.catalog.CatalogService;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.catalog.dto.BookRequest;
import biblioteca.gorbits.catalog.dto.CategoryRequest;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Prueba unitaria de {@link CatalogService} con dependencias mockeadas (Mockito).
 */
@ExtendWith(MockitoExtension.class)
class CatalogServiceUnitTest {

    @Mock
    BookCategoryRepository categories;

    @Mock
    BookRepository books;

    @InjectMocks
    CatalogService service;

    @Test
    void createCategory_casoExitoso_verificaSave() {
        when(categories.existsByNameIgnoreCase("Historia")).thenReturn(false);
        when(categories.save(any(BookCategory.class))).thenAnswer(inv -> {
            BookCategory c = inv.getArgument(0);
            UnitTestFixtures.setId(c, 42L);
            return c;
        });

        var response = service.createCategory(new CategoryRequest("  Historia  "));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Historia");
        verify(categories, times(1)).save(any(BookCategory.class));
    }

    @Test
    void createCategory_nombreDuplicado_lanzaYNoGuarda() {
        when(categories.existsByNameIgnoreCase("Teología")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(new CategoryRequest("Teología")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");

        verify(categories, never()).save(any());
    }

    @Test
    void listCategories_listaVacia() {
        when(categories.findAll()).thenReturn(Collections.emptyList());
        assertThat(service.listCategories()).isEmpty();
        verify(categories, times(1)).findAll();
    }

    @Test
    void listCategories_ordenaPorNombreIgnorandoMayusculas() {
        when(categories.findAll())
                .thenReturn(List.of(
                        UnitTestFixtures.category(1L, "zebra"),
                        UnitTestFixtures.category(2L, "Alpha")));

        assertThat(service.listCategories()).extracting(r -> r.name()).containsExactly("Alpha", "zebra");
    }

    @Test
    void getCategory_idExistente_devuelveRespuesta() {
        when(categories.findById(1L)).thenReturn(Optional.of(UnitTestFixtures.category(1L, "Historia")));

        var response = service.getCategory(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Historia");
    }

    @Test
    void getCategory_idInexistente_lanzaResourceNotFound() {
        when(categories.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCategory(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_casoExitoso() {
        BookCategory c = UnitTestFixtures.category(1L, "Viejo");
        when(categories.findById(1L)).thenReturn(Optional.of(c));
        when(categories.findByNameIgnoreCase("Nuevo")).thenReturn(Optional.empty());

        var response = service.updateCategory(1L, new CategoryRequest("  Nuevo  "));

        assertThat(response.name()).isEqualTo("Nuevo");
        assertThat(c.getName()).isEqualTo("Nuevo");
    }

    @Test
    void updateCategory_idInexistente_lanzaResourceNotFound() {
        when(categories.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(99L, new CategoryRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_nombreOcupadoPorOtra_lanza() {
        BookCategory actual = UnitTestFixtures.category(1L, "A");
        BookCategory otra = UnitTestFixtures.category(2L, "B");
        when(categories.findById(1L)).thenReturn(Optional.of(actual));
        when(categories.findByNameIgnoreCase("B")).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> service.updateCategory(1L, new CategoryRequest("B")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void updateCategory_mismoNombrePropiaCategoria_permite() {
        BookCategory c = UnitTestFixtures.category(1L, "Historia");
        when(categories.findById(1L)).thenReturn(Optional.of(c));
        when(categories.findByNameIgnoreCase("Historia")).thenReturn(Optional.of(c));

        var response = service.updateCategory(1L, new CategoryRequest("Historia"));

        assertThat(response.name()).isEqualTo("Historia");
    }

    @Test
    void deleteCategory_idInexistente_lanzaResourceNotFound() {
        when(categories.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCategory(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(categories, never()).deleteById(any());
    }

    @Test
    void deleteCategory_conLibros_lanzaIllegalState() {
        when(categories.existsById(1L)).thenReturn(true);
        when(books.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCategory(1L)).isInstanceOf(IllegalStateException.class);
        verify(categories, never()).deleteById(any());
    }

    @Test
    void deleteCategory_sinLibros_elimina() {
        when(categories.existsById(1L)).thenReturn(true);
        when(books.existsByCategoryId(1L)).thenReturn(false);

        service.deleteCategory(1L);

        verify(categories, times(1)).deleteById(1L);
    }

    @Test
    void listBooks_sinFiltro_usaFindAll() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(books.findAllByOrderByTitleAsc()).thenReturn(List.of(UnitTestFixtures.book(1L, cat, "A", new BigDecimal("1.00"))));

        assertThat(service.listBooks(null)).hasSize(1);
        verify(books, never()).findByCategoryIdOrderByTitleAsc(any());
    }

    @Test
    void listBooks_porCategoria_usaFindByCategory() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(books.findByCategoryIdOrderByTitleAsc(1L))
                .thenReturn(List.of(UnitTestFixtures.book(1L, cat, "A", new BigDecimal("1.00"))));

        assertThat(service.listBooks(1L)).hasSize(1);
        verify(books, never()).findAllByOrderByTitleAsc();
    }

    @Test
    void getBook_idExistente_devuelveRespuesta() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var book = UnitTestFixtures.book(5L, cat, "Libro", new BigDecimal("10.00"));
        when(books.findById(5L)).thenReturn(Optional.of(book));

        var response = service.getBook(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.title()).isEqualTo("Libro");
    }

    @Test
    void getBook_idInexistente_lanzaResourceNotFound() {
        when(books.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBook(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBook_categoriaInexistente_lanzaResourceNotFound() {
        when(categories.findById(1L)).thenReturn(Optional.empty());

        var req = new BookRequest(1L, "Libro", new BigDecimal("10.00"), BookType.UNITARIO, null, null, null);
        assertThatThrownBy(() -> service.createBook(req)).isInstanceOf(ResourceNotFoundException.class);
        verify(books, never()).save(any());
    }

    @Test
    void createBook_unitario_sinComplemento() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            UnitTestFixtures.setId(b, 20L);
            return b;
        });

        var req = new BookRequest(1L, "  Libro A  ", new BigDecimal("25.00"), BookType.UNITARIO, "nota", null, null);
        var res = service.createBook(req);

        assertThat(res.title()).isEqualTo("Libro A");
        assertThat(res.companionBookId()).isNull();
        assertThat(res.packageNote()).isNull();
    }

    @Test
    void createBook_paquete_sinComplementoId_lanza() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(categories.findById(1L)).thenReturn(Optional.of(cat));

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota", null, null);
        assertThatThrownBy(() -> service.createBook(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("complemento");
    }

    @Test
    void createBook_paquete_complementoInexistente_lanzaResourceNotFound() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.findById(2L)).thenReturn(Optional.empty());

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota", 2L, null);
        assertThatThrownBy(() -> service.createBook(req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createBook_paquete_complementoNoUnitario_lanza() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var companionPaquete = new Book(cat, "Otro pack", new BigDecimal("5.00"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(companionPaquete, 2L);
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.findById(2L)).thenReturn(Optional.of(companionPaquete));

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota", 2L, null);
        assertThatThrownBy(() -> service.createBook(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unitario");
    }

    @Test
    void createBook_paquete_notaEnBlanco_seNormalizaANull() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var companion = UnitTestFixtures.book(2L, cat, "Comp", new BigDecimal("10.00"));
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.findById(2L)).thenReturn(Optional.of(companion));
        when(books.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            UnitTestFixtures.setId(b, 30L);
            return b;
        });

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "   ", 2L, null);
        var res = service.createBook(req);

        assertThat(res.packageNote()).isNull();
        assertThat(res.companionBookId()).isEqualTo(2L);
    }

    @Test
    void createBook_paquete_sinNota_conComplemento() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var companion = UnitTestFixtures.book(2L, cat, "Comp", new BigDecimal("10.00"));
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.findById(2L)).thenReturn(Optional.of(companion));
        when(books.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            UnitTestFixtures.setId(b, 31L);
            return b;
        });

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, null, 2L, null);
        var res = service.createBook(req);

        assertThat(res.packageNote()).isNull();
        assertThat(res.companionBookId()).isEqualTo(2L);
    }

    @Test
    void createBook_paquete_conComplementoUnitario() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var companion = UnitTestFixtures.book(2L, cat, "Comp", new BigDecimal("10.00"));
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.findById(2L)).thenReturn(Optional.of(companion));
        when(books.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            UnitTestFixtures.setId(b, 30L);
            return b;
        });

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "incluye", 2L, null);
        var res = service.createBook(req);

        assertThat(res.companionBookId()).isEqualTo(2L);
        assertThat(res.companionBookTitle()).isEqualTo("Comp");
    }

    @Test
    void updateBook_casoExitoso() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var existing = UnitTestFixtures.book(10L, cat, "Viejo", new BigDecimal("20.00"));
        var companion = UnitTestFixtures.book(2L, cat, "Comp", new BigDecimal("0.01"));
        when(books.findById(10L)).thenReturn(Optional.of(existing));
        when(categories.findById(1L)).thenReturn(Optional.of(cat));
        when(books.findById(2L)).thenReturn(Optional.of(companion));
        when(books.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new BookRequest(1L, "  Nuevo  ", new BigDecimal("30.00"), BookType.PAQUETE, "nota", 2L, null);
        var res = service.updateBook(10L, req);

        assertThat(res.title()).isEqualTo("Nuevo");
        assertThat(res.price()).isEqualByComparingTo("30.00");
    }

    @Test
    void updateBook_libroInexistente_lanzaResourceNotFound() {
        when(books.findById(99L)).thenReturn(Optional.empty());

        var req = new BookRequest(1L, "X", new BigDecimal("1.00"), BookType.UNITARIO, null, null, null);
        assertThatThrownBy(() -> service.updateBook(99L, req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateBook_categoriaInexistente_lanzaResourceNotFound() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(books.findById(10L)).thenReturn(Optional.of(UnitTestFixtures.book(10L, cat, "A", new BigDecimal("1.00"))));
        when(categories.findById(2L)).thenReturn(Optional.empty());

        var req = new BookRequest(2L, "X", new BigDecimal("1.00"), BookType.UNITARIO, null, null, null);
        assertThatThrownBy(() -> service.updateBook(10L, req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateBook_paquete_complementoEsElMismoLibro_lanza() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var existing = UnitTestFixtures.book(10L, cat, "Unitario", new BigDecimal("50.00"));
        var companionMismoId = UnitTestFixtures.book(10L, cat, "Otro unitario", new BigDecimal("1.00"));
        when(books.findById(10L)).thenReturn(Optional.of(existing), Optional.of(companionMismoId));
        when(categories.findById(1L)).thenReturn(Optional.of(cat));

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota", 10L, null);
        assertThatThrownBy(() -> service.updateBook(10L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mismo libro");
    }

    @Test
    void deleteBook_idInexistente_lanzaResourceNotFound() {
        when(books.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteBook(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(books, never()).deleteById(any());
    }

    @Test
    void deleteBook_idExistente_elimina() {
        when(books.existsById(5L)).thenReturn(true);

        service.deleteBook(5L);

        verify(books, times(1)).deleteById(5L);
    }

    @Test
    void validatePackageNote_paqueteSinComplemento_lanza() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var pack = new Book(cat, "Pack **", new BigDecimal("50.00"), BookType.PAQUETE, "incluye");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "validatePackageNote", pack))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("complemento configurado");
    }
}
