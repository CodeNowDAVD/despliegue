package biblioteca.gorbits.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.SalesGuideLineRepository;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderStockServiceTest {

    @Mock
    LibrarySupplyInvoiceLineRepository invoiceLines;

    @Mock
    LibraryStockReturnLineRepository returnLines;

    @Mock
    SalesGuideLineRepository guideLines;

    @Mock
    BookRepository books;

    @InjectMocks
    ProviderStockService service;

    private final UserAccount owner = UnitTestFixtures.proveedor(7L);

    @Test
    void availableUnits_calculaCompradoMenosDevueltoYVendido() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 100L)).thenReturn(50);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 100L)).thenReturn(10);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 100L)).thenReturn(25);

        assertThat(service.availableUnits(owner, 100L)).isEqualTo(15);
    }

    @Test
    void availableUnits_noNegativo() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 1L)).thenReturn(5);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 1L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 1L)).thenReturn(20);

        assertThat(service.availableUnits(owner, 1L)).isZero();
    }

    @Test
    void ensureAvailable_rechazaStockInsuficiente() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 5L)).thenReturn(2);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 5L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 5L)).thenReturn(0);
        var cat = UnitTestFixtures.category(1L, "C");
        var book = UnitTestFixtures.book(5L, cat, "Biblia", java.math.BigDecimal.TEN);
        when(books.findById(5L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.ensureAvailable(owner, Map.of(5L, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void ensureAvailable_cantidadInvalida() {
        assertThatThrownBy(() -> service.ensureAvailable(owner, Map.of(1L, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cantidad inválida");
    }

    @Test
    void allocateLibraryReturn_fifoPorLineasDeFactura() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 3L)).thenReturn(10);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 3L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 3L)).thenReturn(0);

        var cat = UnitTestFixtures.category(1L, "C");
        var book = UnitTestFixtures.book(3L, cat, "Libro", java.math.BigDecimal.ONE);
        var line1 = new LibrarySupplyInvoiceLine(null, book, 5, java.math.BigDecimal.TEN);
        UnitTestFixtures.setId(line1, 101L);
        var line2 = new LibrarySupplyInvoiceLine(null, book, 5, java.math.BigDecimal.TEN);
        UnitTestFixtures.setId(line2, 102L);

        when(invoiceLines.findReturnableLinesForOwnerAndBook(7L, 3L)).thenReturn(List.of(line1, line2));
        when(returnLines.sumReturnedQuantityForInvoiceLine(101L)).thenReturn(0);
        when(returnLines.sumReturnedQuantityForInvoiceLine(102L)).thenReturn(0);

        var allocations = service.allocateLibraryReturn(owner, 3L, 7);

        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).quantity()).isEqualTo(5);
        assertThat(allocations.get(1).quantity()).isEqualTo(2);
    }

    @Test
    void ensureAvailable_mapaVacio_noHaceNada() {
        service.ensureAvailable(owner, Map.of());
    }

    @Test
    void ensureAvailable_stockSuficiente_ok() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 8L)).thenReturn(20);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 8L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 8L)).thenReturn(5);

        service.ensureAvailable(owner, Map.of(8L, 10));
    }

    @Test
    void ensureAvailable_libroInexistente_enMensajeError() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 9L)).thenReturn(0);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 9L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 9L)).thenReturn(0);
        when(books.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ensureAvailable(owner, Map.of(9L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void allocateLibraryReturn_cantidadInvalida() {
        assertThatThrownBy(() -> service.allocateLibraryReturn(owner, 1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    void allocateLibraryReturn_omiteLineasAgotadas() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 4L)).thenReturn(5);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 4L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 4L)).thenReturn(0);

        var book = UnitTestFixtures.book(4L, UnitTestFixtures.category(1L, "C"), "Lib", java.math.BigDecimal.ONE);
        var agotada = new LibrarySupplyInvoiceLine(null, book, 3, java.math.BigDecimal.TEN);
        UnitTestFixtures.setId(agotada, 201L);
        var activa = new LibrarySupplyInvoiceLine(null, book, 5, java.math.BigDecimal.TEN);
        UnitTestFixtures.setId(activa, 202L);

        when(invoiceLines.findReturnableLinesForOwnerAndBook(7L, 4L)).thenReturn(List.of(agotada, activa));
        when(returnLines.sumReturnedQuantityForInvoiceLine(201L)).thenReturn(3);
        when(returnLines.sumReturnedQuantityForInvoiceLine(202L)).thenReturn(0);

        var allocations = service.allocateLibraryReturn(owner, 4L, 2);

        assertThat(allocations).hasSize(1);
        assertThat(allocations.getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void stockByBook_agrupaComprasDevolucionesYVentas() {
        List<Object[]> purchasedRows = new ArrayList<>();
        purchasedRows.add(new Object[] {10L, "Libro A", 1L, "Cat", 20});
        List<Object[]> returnedRows = new ArrayList<>();
        returnedRows.add(new Object[] {10L, 3});
        List<Object[]> soldRows = new ArrayList<>();
        soldRows.add(new Object[] {10L, 5});
        when(invoiceLines.sumPurchasedByBookForOwner(7L)).thenReturn(purchasedRows);
        when(returnLines.sumReturnedByBookForOwner(7L)).thenReturn(returnedRows);
        when(guideLines.sumSoldByBookForOwner(7L)).thenReturn(soldRows);

        var rows = service.stockByBook(owner);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().purchased()).isEqualTo(20);
        assertThat(rows.getFirst().returnedToLibrary()).isEqualTo(3);
        assertThat(rows.getFirst().soldOnContracts()).isEqualTo(5);
        assertThat(rows.getFirst().available()).isEqualTo(12);
    }

    @Test
    void allocateLibraryReturn_excedeDisponible() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 1L)).thenReturn(3);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 1L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 1L)).thenReturn(0);
        var book = UnitTestFixtures.book(1L, UnitTestFixtures.category(1L, "C"), "X", java.math.BigDecimal.ONE);
        when(books.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.allocateLibraryReturn(owner, 1L, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo");
    }

    @Test
    void allocateLibraryReturn_sinCapacidadEnLineas_lanza() {
        when(invoiceLines.sumPurchasedQtyByOwnerAndBook(7L, 6L)).thenReturn(10);
        when(returnLines.sumReturnedQtyByOwnerAndBook(7L, 6L)).thenReturn(0);
        when(guideLines.sumSoldQtyByOwnerAndBook(7L, 6L)).thenReturn(0);

        var book = UnitTestFixtures.book(6L, UnitTestFixtures.category(1L, "C"), "Lib", java.math.BigDecimal.ONE);
        var line = new LibrarySupplyInvoiceLine(null, book, 3, java.math.BigDecimal.TEN);
        UnitTestFixtures.setId(line, 301L);

        when(invoiceLines.findReturnableLinesForOwnerAndBook(7L, 6L)).thenReturn(List.of(line));
        when(returnLines.sumReturnedQuantityForInvoiceLine(301L)).thenReturn(0);

        assertThatThrownBy(() -> service.allocateLibraryReturn(owner, 6L, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo asignar");
    }

    @Test
    void stockByBook_ignoraDevolucionesYVentasSinCompraPrevio() {
        List<Object[]> purchasedRows = new ArrayList<>();
        purchasedRows.add(new Object[] {10L, "Libro A", 1L, "Cat", 20});
        List<Object[]> returnedRows = new ArrayList<>();
        returnedRows.add(new Object[] {99L, 3});
        List<Object[]> soldRows = new ArrayList<>();
        soldRows.add(new Object[] {88L, 5});
        when(invoiceLines.sumPurchasedByBookForOwner(7L)).thenReturn(purchasedRows);
        when(returnLines.sumReturnedByBookForOwner(7L)).thenReturn(returnedRows);
        when(guideLines.sumSoldByBookForOwner(7L)).thenReturn(soldRows);

        var rows = service.stockByBook(owner);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().bookId()).isEqualTo(10L);
    }
}
