package biblioteca.gorbits.unit.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.inventory.InventoryMovementLogger;
import biblioteca.gorbits.inventory.InventoryMovementRepository;
import biblioteca.gorbits.inventory.InventoryMovementType;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.LibraryStockReturnRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoice;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceLineRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceRepository;
import biblioteca.gorbits.inventory.LibraryPaymentRepository;
import biblioteca.gorbits.inventory.LibraryStockReturnLineRepository;
import biblioteca.gorbits.inventory.ProviderFieldStockRepository;
import biblioteca.gorbits.inventory.ProviderStockService;
import biblioteca.gorbits.inventory.StockWithdrawalRepository;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
import biblioteca.gorbits.inventory.dto.CreateLibrarySupplyInvoiceRequest;
import biblioteca.gorbits.inventory.dto.LibrarySupplyLineItemRequest;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceLibraryUnitTest {

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

    private final UserAccount proveedor = UnitTestFixtures.proveedor(1L);
    private final UserAccount admin = UnitTestFixtures.admin(2L);

    @Test
    void listLibrarySupplyInvoices_adminSinFiltro() {
        var inv = new LibrarySupplyInvoice(proveedor, "F-1", LocalDate.of(2026, 3, 1), null, Instant.now());
        UnitTestFixtures.setId(inv, 10L);
        inv.addLine(UnitTestFixtures.book(5L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("100")), 5, new BigDecimal("500"));
        when(libraryInvoices.findAllWithLines()).thenReturn(List.of(inv));

        var list = service.listLibrarySupplyInvoices(admin, null);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().ownerUsername()).isEqualTo("proveedor");
        assertThat(list.getFirst().totalUnits()).isEqualTo(5);
    }

    @Test
    void listLibrarySupplyInvoices_adminConProveedor() {
        when(libraryInvoices.findAllWithLinesByOwner_Id(1L)).thenReturn(List.of());

        assertThat(service.listLibrarySupplyInvoices(admin, 1L)).isEmpty();
    }

    @Test
    void listLibrarySupplyInvoices_proveedorIgnoraFiltro() {
        when(libraryInvoices.findAllWithLinesByOwner_Id(1L)).thenReturn(List.of());
        assertThat(service.listLibrarySupplyInvoices(proveedor, 99L)).isEmpty();
    }

    @Test
    void getLibrarySupplyInvoice_admin() {
        var inv = new LibrarySupplyInvoice(proveedor, "F-2", LocalDate.of(2026, 3, 2), "nota", Instant.now());
        UnitTestFixtures.setId(inv, 20L);
        inv.addLine(UnitTestFixtures.book(6L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10")), 1, new BigDecimal("10"));
        when(libraryInvoices.findDetailedById(20L)).thenReturn(Optional.of(inv));

        var detail = service.getLibrarySupplyInvoice(admin, 20L);

        assertThat(detail.invoiceNumber()).isEqualTo("F-2");
        assertThat(detail.lines()).hasSize(1);
    }

    @Test
    void getLibrarySupplyInvoice_proveedor_noEncontrada() {
        when(libraryInvoices.findDetailedByIdAndOwner_Id(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLibrarySupplyInvoice(proveedor, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerLibrarySupplyInvoice_proveedor() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("20.00"));
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-U-1")).thenReturn(false);
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 30L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(30L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-U-1",
                LocalDate.of(2026, 3, 10),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 4, null)),
                null);

        var detail = service.registerLibrarySupplyInvoice(proveedor, req);

        assertThat(detail.invoiceNumber()).isEqualTo("FAC-U-1");
        assertThat(detail.totalUnits()).isEqualTo(4);
        verify(movementLogger)
                .log(
                        eq(proveedor),
                        eq(book),
                        eq(InventoryMovementType.LIBRARY_INVOICE_ENTRY),
                        anyInt(),
                        anyInt(),
                        eq("LIBRARY_SUPPLY_INVOICE"),
                        eq(30L),
                        any(),
                        any());
    }

    @Test
    void getLibrarySupplyInvoice_proveedor_ok() {
        var inv = new LibrarySupplyInvoice(proveedor, "F-3", LocalDate.of(2026, 4, 1), null, Instant.now());
        UnitTestFixtures.setId(inv, 25L);
        when(libraryInvoices.findDetailedByIdAndOwner_Id(25L, 1L)).thenReturn(Optional.of(inv));

        assertThat(service.getLibrarySupplyInvoice(proveedor, 25L).invoiceNumber()).isEqualTo("F-3");
    }

    @Test
    void listLibrarySupplyInvoices_rolNoSoportado() {
        var sinRol = UnitTestFixtures.user(9L, "x", Role.ADMIN);
        ReflectionTestUtils.setField(sinRol, "role", null);
        assertThatThrownBy(() -> service.listLibrarySupplyInvoices(sinRol, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol no soportado");
    }

    @Test
    void getLibrarySupplyInvoice_rolNoSoportado() {
        var sinRol = UnitTestFixtures.user(9L, "x", Role.ADMIN);
        ReflectionTestUtils.setField(sinRol, "role", null);
        assertThatThrownBy(() -> service.getLibrarySupplyInvoice(sinRol, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol no soportado");
    }

    @Test
    void registerLibrarySupplyInvoice_adminSinOwnerUserId() {
        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-ADM", LocalDate.now(), null, List.of(new LibrarySupplyLineItemRequest(1L, 1, null)), null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(admin, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerUserId");
    }

    @Test
    void registerLibrarySupplyInvoice_proveedorOtroOwner() {
        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-BAD", LocalDate.now(), null, List.of(new LibrarySupplyLineItemRequest(1L, 1, null)), 99L);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("otro proveedor");
    }

    @Test
    void registerLibrarySupplyInvoice_adminProveedorNoEncontrado() {
        when(userAccounts.findByIdAndRole(99L, Role.PROVEEDOR)).thenReturn(Optional.empty());
        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-ADM2", LocalDate.now(), null, List.of(new LibrarySupplyLineItemRequest(1L, 1, null)), 99L);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(admin, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proveedor no encontrado");
    }

    @Test
    void registerLibrarySupplyInvoice_paqueteSinComplemento_rechaza() {
        var cat = UnitTestFixtures.category(1L, "C");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-PAQ")).thenReturn(false);
        when(books.findById(12L)).thenReturn(Optional.of(pack));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-PAQ",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(12L, 5, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe ir con");
    }

    @Test
    void registerLibrarySupplyInvoice_paqueteCantidadesDistintas_rechaza() {
        var cat = UnitTestFixtures.category(1L, "C");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-PAQ2")).thenReturn(false);
        when(books.findById(12L)).thenReturn(Optional.of(pack));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-PAQ2",
                LocalDate.now(),
                null,
                List.of(
                        new LibrarySupplyLineItemRequest(12L, 5, null),
                        new LibrarySupplyLineItemRequest(11L, 3, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe tener 5 unidades");
    }

    @Test
    void registerLibrarySupplyInvoice_numeroDuplicado() {
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "DUP")).thenReturn(true);
        var req = new CreateLibrarySupplyInvoiceRequest(
                "DUP",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(1L, 1, null)),
                null);

        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("número");
    }

    @Test
    void registerLibrarySupplyInvoice_numeroVacio_rechaza() {
        var req = new CreateLibrarySupplyInvoiceRequest(
                "   ",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("número de factura");
    }

    @Test
    void registerLibrarySupplyInvoice_libroNoEncontrado_enValidacion() {
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-NF")).thenReturn(false);
        when(books.findById(99L)).thenReturn(Optional.empty());

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-NF",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(99L, 1, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Libro no encontrado");
    }

    @Test
    void registerLibrarySupplyInvoice_admin_asignaProveedor() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("25.00"));
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(userAccounts.findByIdAndRole(1L, Role.PROVEEDOR)).thenReturn(Optional.of(proveedor));
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-ADM-OK")).thenReturn(false);
        stubValidateBook(book);
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 40L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(40L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-ADM-OK",
                LocalDate.of(2026, 3, 15),
                "nota",
                List.of(new LibrarySupplyLineItemRequest(10L, 2, new BigDecimal("10"))),
                1L);

        var detail = service.registerLibrarySupplyInvoice(admin, req);

        assertThat(detail.ownerId()).isEqualTo(1L);
        assertThat(detail.totalUnits()).isEqualTo(2);
        assertThat(detail.totalAmount()).isEqualByComparingTo("45.00");
    }

    @Test
    void registerLibrarySupplyInvoice_conDescuentoYNotaVacia() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("100.00"));
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-DISC")).thenReturn(false);
        stubValidateBook(book);
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 41L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(41L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-DISC",
                LocalDate.now(),
                "   ",
                List.of(new LibrarySupplyLineItemRequest(10L, 2, new BigDecimal("10"))),
                null);

        var detail = service.registerLibrarySupplyInvoice(proveedor, req);

        assertThat(detail.note()).isNull();
        assertThat(detail.totalAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void registerLibrarySupplyInvoice_descuentoInvalido_rechaza() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-BAD-DISC")).thenReturn(false);
        stubValidateBook(book);

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-BAD-DISC",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, new BigDecimal("101"))),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Descuento inválido");
    }

    @Test
    void registerLibrarySupplyInvoice_paqueteConComplemento_ok() {
        var cat = UnitTestFixtures.category(1L, "C");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-PAQ-OK")).thenReturn(false);
        when(books.findById(12L)).thenReturn(Optional.of(pack));
        when(books.findById(11L)).thenReturn(Optional.of(companion));
        when(books.findByCompanionBook_Id(anyLong())).thenReturn(List.of());
        when(books.findByCompanionBook_Id(11L)).thenReturn(List.of(pack));
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 42L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(42L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-PAQ-OK",
                LocalDate.now(),
                null,
                List.of(
                        new LibrarySupplyLineItemRequest(12L, 4, null),
                        new LibrarySupplyLineItemRequest(11L, 4, null)),
                null);

        assertThat(service.registerLibrarySupplyInvoice(proveedor, req).totalUnits()).isEqualTo(8);
    }

    @Test
    void registerLibrarySupplyInvoice_complementoSinPaquete_rechaza() {
        var cat = UnitTestFixtures.category(1L, "C");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-COMP-SOLO")).thenReturn(false);
        when(books.findById(11L)).thenReturn(Optional.of(companion));
        when(books.findByCompanionBook_Id(11L)).thenReturn(List.of(pack));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-COMP-SOLO",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(11L, 5, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("solo se factura junto");
    }

    @Test
    void registerLibrarySupplyInvoice_complementoCantidadDistintaSegundoBucle_rechaza() {
        var cat = UnitTestFixtures.category(1L, "C");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var pack = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(pack, 12L);
        pack.setCompanionBook(companion);
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-MISMATCH")).thenReturn(false);
        when(books.findById(11L)).thenReturn(Optional.of(companion));
        when(books.findById(12L)).thenReturn(Optional.of(pack));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-MISMATCH",
                LocalDate.now(),
                null,
                List.of(
                        new LibrarySupplyLineItemRequest(11L, 5, null),
                        new LibrarySupplyLineItemRequest(12L, 3, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe tener")
                .hasMessageContaining("3 unidades");
    }

    @Test
    void registerLibrarySupplyInvoice_detalleNoEncontradoTrasGuardar() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-NO-DET")).thenReturn(false);
        stubValidateBook(book);
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 43L);
            return invc;
        });
        when(libraryInvoices.findDetailedById(43L)).thenReturn(Optional.empty());

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-NO-DET",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void getLibrarySupplyInvoice_admin_noEncontrada() {
        when(libraryInvoices.findDetailedById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLibrarySupplyInvoice(admin, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerLibrarySupplyInvoice_resolveInvoiceOwner_rolNoAutorizado() {
        var sinRol = UnitTestFixtures.user(9L, "x", Role.ADMIN);
        ReflectionTestUtils.setField(sinRol, "role", null);
        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-ROL",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(sinRol, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No autorizado");
    }

    @Test
    void registerLibrarySupplyInvoice_libroNoEncontrado_enSegundoBucleValidacion() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        AtomicInteger findCalls = new AtomicInteger();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-2ND")).thenReturn(false);
        when(books.findById(10L)).thenAnswer(inv -> findCalls.incrementAndGet() == 1 ? Optional.of(book) : Optional.empty());

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-2ND",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerLibrarySupplyInvoice_libroNoEncontrado_enResolveLineTotals() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        AtomicInteger findCalls = new AtomicInteger();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-RES")).thenReturn(false);
        when(books.findById(10L))
                .thenAnswer(inv -> findCalls.incrementAndGet() <= 2 ? Optional.of(book) : Optional.empty());

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-RES",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerLibrarySupplyInvoice_descuentoCero_usaPrecioCompleto() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("50.00"));
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-ZERO-DISC")).thenReturn(false);
        stubValidateBook(book);
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 44L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(44L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-ZERO-DISC",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 2, BigDecimal.ZERO)),
                null);

        assertThat(service.registerLibrarySupplyInvoice(proveedor, req).totalAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void registerLibrarySupplyInvoice_proveedorConSuPropioOwnerUserId() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-OWN")).thenReturn(false);
        stubValidateBook(book);
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 45L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(45L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-OWN",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, null)),
                1L);

        assertThat(service.registerLibrarySupplyInvoice(proveedor, req).invoiceNumber()).isEqualTo("FAC-OWN");
    }

    @Test
    void registerLibrarySupplyInvoice_cantidadCero_rechaza() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-QTY0")).thenReturn(false);
        stubValidateBook(book);

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-QTY0",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 0, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cantidad inválida");
    }

    @Test
    void registerLibrarySupplyInvoice_importeNegativo_rechaza() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("-5.00"));
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-NEG")).thenReturn(false);
        stubValidateBook(book);

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-NEG",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 2, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Importe de línea no puede ser negativo");
    }

    @Test
    void registerLibrarySupplyInvoice_descuentoNegativo_rechaza() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-NEG-DISC")).thenReturn(false);
        stubValidateBook(book);

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-NEG-DISC",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(10L, 1, new BigDecimal("-1"))),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Descuento inválido");
    }

    @Test
    void registerLibrarySupplyInvoice_paqueteSinComplementoEnCatalogo_pasaValidacion() {
        var cat = UnitTestFixtures.category(1L, "C");
        var pack = new Book(cat, "Pack sin comp", new BigDecimal("50"), BookType.PAQUETE, "nota");
        UnitTestFixtures.setId(pack, 12L);
        AtomicReference<LibrarySupplyInvoice> saved = new AtomicReference<>();
        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-PAQ-NC")).thenReturn(false);
        when(books.findById(12L)).thenReturn(Optional.of(pack));
        when(books.findByCompanionBook_Id(anyLong())).thenReturn(List.of());
        when(libraryInvoices.save(any(LibrarySupplyInvoice.class))).thenAnswer(inv -> {
            LibrarySupplyInvoice invc = inv.getArgument(0);
            UnitTestFixtures.setId(invc, 46L);
            saved.set(invc);
            return invc;
        });
        when(libraryInvoices.findDetailedById(46L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-PAQ-NC",
                LocalDate.now(),
                null,
                List.of(new LibrarySupplyLineItemRequest(12L, 2, null)),
                null);

        assertThat(service.registerLibrarySupplyInvoice(proveedor, req).totalUnits()).isEqualTo(2);
    }

    @Test
    void registerLibrarySupplyInvoice_segundoBucle_companionCantidadDistinta() {
        var cat = UnitTestFixtures.category(1L, "C");
        var companion = UnitTestFixtures.book(11L, cat, "Comp", new BigDecimal("0.01"));
        var packSinComp = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(packSinComp, 12L);
        var packConComp = new Book(cat, "Pack **", new BigDecimal("50"), BookType.PAQUETE, null);
        UnitTestFixtures.setId(packConComp, 12L);
        packConComp.setCompanionBook(companion);

        when(libraryInvoices.existsByOwner_IdAndInvoiceNumberIgnoreCase(1L, "FAC-2ND-MIS")).thenReturn(false);
        when(books.findById(11L)).thenReturn(Optional.of(companion));
        when(books.findById(12L)).thenReturn(Optional.of(packSinComp));
        when(books.findByCompanionBook_Id(11L)).thenReturn(List.of(packConComp));

        var req = new CreateLibrarySupplyInvoiceRequest(
                "FAC-2ND-MIS",
                LocalDate.now(),
                null,
                List.of(
                        new LibrarySupplyLineItemRequest(11L, 5, null),
                        new LibrarySupplyLineItemRequest(12L, 3, null)),
                null);
        assertThatThrownBy(() -> service.registerLibrarySupplyInvoice(proveedor, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe tener")
                .hasMessageContaining("3 unidades");
    }

    @Test
    void listLibrarySupplyInvoices_lineaSinImporte_usaCero() {
        var inv = new LibrarySupplyInvoice(proveedor, "F-NULL-AMT", LocalDate.now(), null, Instant.now());
        UnitTestFixtures.setId(inv, 50L);
        var book = UnitTestFixtures.book(5L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        inv.addLine(book, 1, null);
        when(libraryInvoices.findAllWithLinesByOwner_Id(1L)).thenReturn(List.of(inv));

        assertThat(service.listLibrarySupplyInvoices(proveedor, null).getFirst().totalAmount())
                .isEqualByComparingTo("0.00");
    }

    private void stubValidateBook(Book book) {
        when(books.findById(book.getId())).thenReturn(Optional.of(book));
        when(books.findByCompanionBook_Id(anyLong())).thenReturn(List.of());
    }
}
