package biblioteca.gorbits.unit.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.config.SeedInventoryInitializer;
import biblioteca.gorbits.inventory.WarehouseStock;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
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
class SeedInventoryInitializerUnitTest {

    @Mock
    BookRepository books;

    @Mock
    WarehouseStockRepository warehouse;

    @InjectMocks
    SeedInventoryInitializer initializer;

    @Test
    void run_sinLibros_noTocaAlmacen() throws Exception {
        when(books.count()).thenReturn(0L);

        initializer.run();

        verify(books, never()).findAll();
        verify(warehouse, never()).save(any());
    }

    @Test
    void run_libroSinStock_creaEntrada() throws Exception {
        var category = UnitTestFixtures.category(1L, "Historia");
        Book book = UnitTestFixtures.book(10L, category, "El Quijote", BigDecimal.TEN);
        when(books.count()).thenReturn(1L);
        when(books.findAll()).thenReturn(List.of(book));
        when(warehouse.findByBook_Id(10L)).thenReturn(Optional.empty());

        initializer.run();

        verify(warehouse).save(any(WarehouseStock.class));
    }

    @Test
    void run_libroConStockExistente_noGuarda() throws Exception {
        var category = UnitTestFixtures.category(1L, "Historia");
        Book book = UnitTestFixtures.book(10L, category, "El Quijote", BigDecimal.TEN);
        var existing = new WarehouseStock(book, 100);
        when(books.count()).thenReturn(1L);
        when(books.findAll()).thenReturn(List.of(book));
        when(warehouse.findByBook_Id(10L)).thenReturn(Optional.of(existing));

        initializer.run();

        verify(warehouse, never()).save(any());
    }
}
