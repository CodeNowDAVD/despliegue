package biblioteca.gorbits.unit.config;

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
import biblioteca.gorbits.config.SeedCatalogInitializer;
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
class SeedCatalogInitializerUnitTest {

    @Mock
    BookCategoryRepository categories;

    @Mock
    BookRepository books;

    @InjectMocks
    SeedCatalogInitializer initializer;

    @Test
    void run_conMarcador_noCargaDeNuevo() throws Exception {
        Book marker = UnitTestFixtures.book(
                1L,
                UnitTestFixtures.category(1L, "Salud"),
                "El maravilloso mundo de la Biblia",
                BigDecimal.TEN);
        when(books.findAllByOrderByTitleAsc()).thenReturn(List.of(marker));

        initializer.run();

        verify(books, never()).save(any());
    }

    @Test
    void run_sinMarcador_cargaCatalogo() throws Exception {
        when(books.findAllByOrderByTitleAsc()).thenReturn(List.of());
        when(categories.findByNameIgnoreCase("Salud")).thenReturn(Optional.empty());
        when(categories.findByNameIgnoreCase("Familia")).thenReturn(Optional.empty());
        when(categories.save(any(BookCategory.class)))
                .thenAnswer(inv -> {
                    BookCategory c = inv.getArgument(0);
                    if ("Salud".equals(c.getName())) {
                        UnitTestFixtures.setId(c, 1L);
                    } else {
                        UnitTestFixtures.setId(c, 2L);
                    }
                    return c;
                });
        when(books.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            if (b.getBookType() == BookType.PAQUETE) {
                UnitTestFixtures.setId(b, 99L);
            }
            return b;
        });

        initializer.run();

        verify(books, times(6)).save(any(Book.class));
    }

    @Test
    void run_categoriaExistente_reutiliza() throws Exception {
        var salud = UnitTestFixtures.category(1L, "Salud");
        when(books.findAllByOrderByTitleAsc()).thenReturn(List.of());
        when(categories.findByNameIgnoreCase("Salud")).thenReturn(Optional.of(salud));
        when(categories.findByNameIgnoreCase("Familia")).thenReturn(Optional.empty());
        when(categories.save(any(BookCategory.class))).thenAnswer(inv -> {
            BookCategory c = inv.getArgument(0);
            UnitTestFixtures.setId(c, 2L);
            return c;
        });
        when(books.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        initializer.run();

        verify(categories, never()).save(salud);
    }
}
