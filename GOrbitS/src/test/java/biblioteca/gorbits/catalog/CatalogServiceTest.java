package biblioteca.gorbits.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.dto.BookRequest;
import biblioteca.gorbits.catalog.dto.CategoryRequest;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    BookCategoryRepository categories;

    @Mock
    BookRepository books;

    @InjectMocks
    CatalogService service;

    @Test
    void createCategory_rechazaNombreDuplicado() {
        when(categories.existsByNameIgnoreCase("Teología")).thenReturn(true);
        assertThatThrownBy(() -> service.createCategory(new CategoryRequest("Teología")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createCategory_guardaNueva() {
        when(categories.existsByNameIgnoreCase("Historia")).thenReturn(false);
        when(categories.save(any(BookCategory.class))).thenAnswer(inv -> {
            BookCategory c = inv.getArgument(0);
            UnitTestFixtures.setId(c, 10L);
            return c;
        });

        var res = service.createCategory(new CategoryRequest("  Historia  "));
        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.name()).isEqualTo("Historia");
    }

    @Test
    void deleteCategory_conLibros_lanza() {
        when(categories.existsById(1L)).thenReturn(true);
        when(books.existsByCategoryId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteCategory(1L)).isInstanceOf(IllegalStateException.class);
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

        var req = new BookRequest(1L, "Libro A", new BigDecimal("25.00"), BookType.UNITARIO, null, null, null);
        var res = service.createBook(req);

        assertThat(res.title()).isEqualTo("Libro A");
        assertThat(res.companionBookId()).isNull();
    }

    @Test
    void createBook_paquete_requiereComplemento() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        when(categories.findById(1L)).thenReturn(Optional.of(cat));

        var req = new BookRequest(1L, "Pack", new BigDecimal("50.00"), BookType.PAQUETE, "nota", null, null);
        assertThatThrownBy(() -> service.createBook(req)).isInstanceOf(IllegalArgumentException.class);
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
    }

    @Test
    void listBooks_filtraPorCategoria() {
        var cat = UnitTestFixtures.category(1L, "Cat");
        var b = UnitTestFixtures.book(1L, cat, "A", new BigDecimal("1.00"));
        when(books.findByCategoryIdOrderByTitleAsc(1L)).thenReturn(List.of(b));

        assertThat(service.listBooks(1L)).hasSize(1);
        verify(books, never()).findAllByOrderByTitleAsc();
    }

    @Test
    void getCategory_noEncontrada() {
        when(categories.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCategory(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
