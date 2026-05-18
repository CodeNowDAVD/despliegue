package biblioteca.gorbits.unit.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.inventory.InventoryMovementLogger;
import biblioteca.gorbits.inventory.InventoryMovementRepository;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.LibraryPaymentRepository;
import biblioteca.gorbits.inventory.LibraryStockReturnLineRepository;
import biblioteca.gorbits.inventory.LibraryStockReturnRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceLineRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceRepository;
import biblioteca.gorbits.inventory.ProviderFieldStock;
import biblioteca.gorbits.inventory.ProviderFieldStockRepository;
import biblioteca.gorbits.inventory.ProviderStockService;
import biblioteca.gorbits.inventory.StockWithdrawal;
import biblioteca.gorbits.inventory.StockWithdrawalRepository;
import biblioteca.gorbits.inventory.WarehouseStock;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
import biblioteca.gorbits.inventory.dto.CreateWithdrawalRequest;
import biblioteca.gorbits.inventory.dto.WithdrawalLineRequest;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceWithdrawalUnitTest {

    @Mock
    BookRepository books;
    @Mock
    WarehouseStockRepository warehouse;
    @Mock
    ProviderFieldStockRepository field;
    @Mock
    StockWithdrawalRepository withdrawals;
    @Mock
    LibrarySupplyInvoiceRepository libraryInvoices;
    @Mock
    LibraryStockReturnRepository libraryStockReturns;
    @Mock
    CampaignRepository campaigns;
    @Mock
    SalesGuideRepository salesGuides;
    @Mock
    LibraryPaymentRepository libraryPayments;
    @Mock
    UserAccountRepository userAccounts;
    @Mock
    LibrarySupplyInvoiceLineRepository invoiceLines;
    @Mock
    LibraryStockReturnLineRepository libraryReturnLines;
    @Mock
    InventoryMovementRepository movements;
    @Mock
    InventoryMovementLogger movementLogger;
    @Mock
    ProviderStockService providerStock;

    @InjectMocks
    InventoryService service;

    @Test
    void registerWithdrawal_casoExitoso() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Libro", new BigDecimal("1"));
        var ws = new WarehouseStock(book, 100);
        var saved = new StockWithdrawal(owner, java.time.Instant.now(), "nota");
        UnitTestFixtures.setId(saved, 50L);
        saved.addLine(book, 5);

        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(warehouse.findForUpdateByBook_Id(10L)).thenReturn(Optional.of(ws));
        when(field.findByOwner_IdAndBook_Id(1L, 10L)).thenReturn(Optional.empty());
        when(field.save(any(ProviderFieldStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(withdrawals.save(any(StockWithdrawal.class))).thenAnswer(inv -> {
            StockWithdrawal w = inv.getArgument(0);
            UnitTestFixtures.setId(w, 50L);
            return w;
        });
        when(withdrawals.findDetailedByIdAndOwner_Id(eq(50L), eq(1L))).thenReturn(Optional.of(saved));

        var req = new CreateWithdrawalRequest("nota", List.of(new WithdrawalLineRequest(10L, 5)));
        var detail = service.registerWithdrawal(owner, req);

        assertThat(detail.totalUnits()).isEqualTo(5);
        assertThat(ws.getQuantity()).isEqualTo(95);
        verify(movementLogger).log(any(), any(), any(), anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    void listWithdrawals_mapea() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Libro", BigDecimal.ONE);
        var w = new StockWithdrawal(owner, Instant.parse("2026-03-01T10:00:00Z"), "nota");
        UnitTestFixtures.setId(w, 5L);
        w.addLine(book, 3);
        when(withdrawals.findByOwner_IdWithLines(1L)).thenReturn(List.of(w));

        var list = service.listWithdrawals(owner);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().totalUnits()).isEqualTo(3);
    }

    @Test
    void getWithdrawal_ok() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Libro", BigDecimal.ONE);
        var w = new StockWithdrawal(owner, Instant.now(), null);
        UnitTestFixtures.setId(w, 8L);
        w.addLine(book, 2);
        when(withdrawals.findDetailedByIdAndOwner_Id(8L, 1L)).thenReturn(Optional.of(w));

        var detail = service.getWithdrawal(owner, 8L);

        assertThat(detail.id()).isEqualTo(8L);
        assertThat(detail.lines()).hasSize(1);
    }

    @Test
    void getWithdrawal_noEncontrado() {
        var owner = UnitTestFixtures.proveedor(1L);
        when(withdrawals.findDetailedByIdAndOwner_Id(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getWithdrawal(owner, 99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerWithdrawal_notaVacia_seNormalizaANull() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Libro", new BigDecimal("1"));
        var ws = new WarehouseStock(book, 100);
        var saved = new StockWithdrawal(owner, Instant.now(), null);
        UnitTestFixtures.setId(saved, 52L);
        saved.addLine(book, 1);

        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(warehouse.findForUpdateByBook_Id(10L)).thenReturn(Optional.of(ws));
        when(field.findByOwner_IdAndBook_Id(1L, 10L)).thenReturn(Optional.empty());
        when(field.save(any(ProviderFieldStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(withdrawals.save(any(StockWithdrawal.class))).thenAnswer(inv -> {
            StockWithdrawal w = inv.getArgument(0);
            UnitTestFixtures.setId(w, 52L);
            return w;
        });
        when(withdrawals.findDetailedByIdAndOwner_Id(52L, 1L)).thenReturn(Optional.of(saved));

        var req = new CreateWithdrawalRequest("   ", List.of(new WithdrawalLineRequest(10L, 1)));
        var detail = service.registerWithdrawal(owner, req);

        assertThat(detail.note()).isNull();
    }

    @Test
    void registerWithdrawal_libroNoEncontradoEnAlmacen_lanza() {
        var owner = UnitTestFixtures.proveedor(1L);
        when(books.findById(10L)).thenReturn(Optional.empty());

        var req = new CreateWithdrawalRequest(null, List.of(new WithdrawalLineRequest(10L, 1)));
        assertThatThrownBy(() -> service.registerWithdrawal(owner, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Libro no encontrado");
    }

    @Test
    void registerWithdrawal_cantidadInvalida() {
        var owner = UnitTestFixtures.proveedor(1L);
        var req = new CreateWithdrawalRequest(null, List.of(new WithdrawalLineRequest(10L, 0)));
        assertThatThrownBy(() -> service.registerWithdrawal(owner, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cantidad inválida");
    }

    @Test
    void registerWithdrawal_detalleNoEncontradoTrasGuardar() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Libro", new BigDecimal("1"));
        var ws = new WarehouseStock(book, 100);

        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(warehouse.findForUpdateByBook_Id(10L)).thenReturn(Optional.of(ws));
        when(field.findByOwner_IdAndBook_Id(1L, 10L)).thenReturn(Optional.empty());
        when(field.save(any(ProviderFieldStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(withdrawals.save(any(StockWithdrawal.class))).thenAnswer(inv -> {
            StockWithdrawal w = inv.getArgument(0);
            UnitTestFixtures.setId(w, 51L);
            return w;
        });
        when(withdrawals.findDetailedByIdAndOwner_Id(51L, 1L)).thenReturn(Optional.empty());

        var req = new CreateWithdrawalRequest(null, List.of(new WithdrawalLineRequest(10L, 1)));
        assertThatThrownBy(() -> service.registerWithdrawal(owner, req))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void registerWithdrawal_stockInsuficienteEnAlmacen() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Libro", BigDecimal.ONE);
        var ws = new WarehouseStock(book, 2);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(warehouse.findForUpdateByBook_Id(10L)).thenReturn(Optional.of(ws));

        var req = new CreateWithdrawalRequest(null, List.of(new WithdrawalLineRequest(10L, 5)));
        assertThatThrownBy(() -> service.registerWithdrawal(owner, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock insuficiente");
    }
}
