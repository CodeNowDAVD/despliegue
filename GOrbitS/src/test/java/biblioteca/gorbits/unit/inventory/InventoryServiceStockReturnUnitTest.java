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
import biblioteca.gorbits.inventory.InventoryMovementType;
import biblioteca.gorbits.inventory.InventoryService;
import biblioteca.gorbits.inventory.LibraryStockReturn;
import biblioteca.gorbits.inventory.LibraryStockReturnLineRepository;
import biblioteca.gorbits.inventory.LibraryStockReturnRepository;
import biblioteca.gorbits.inventory.LibraryPaymentRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceLine;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceLineRepository;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceRepository;
import biblioteca.gorbits.inventory.ProviderFieldStockRepository;
import biblioteca.gorbits.inventory.ProviderStockService;
import biblioteca.gorbits.inventory.ProviderStockService.InvoiceLineAllocation;
import biblioteca.gorbits.inventory.StockWithdrawalRepository;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
import biblioteca.gorbits.inventory.dto.CreateLibraryStockReturnRequest;
import biblioteca.gorbits.inventory.dto.LibraryStockReturnLineItemRequest;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceStockReturnUnitTest {

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

    private final UserAccount owner = UnitTestFixtures.proveedor(1L);

    @Test
    void registerLibraryStockReturn_conCampaña() {
        var campaign = UnitTestFixtures.campaign(5L, "Verano");
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        var invLine = new LibrarySupplyInvoiceLine(null, book, 10, new BigDecimal("100"));
        UnitTestFixtures.setId(invLine, 100L);
        AtomicReference<LibraryStockReturn> saved = new AtomicReference<>();

        when(campaigns.findById(5L)).thenReturn(Optional.of(campaign));
        when(providerStock.allocateLibraryReturn(owner, 10L, 4))
                .thenReturn(List.of(new InvoiceLineAllocation(invLine, 4)));
        when(libraryStockReturns.save(any(LibraryStockReturn.class))).thenAnswer(inv -> {
            LibraryStockReturn r = inv.getArgument(0);
            UnitTestFixtures.setId(r, 50L);
            saved.set(r);
            return r;
        });
        when(libraryStockReturns.findDetailedByIdAndOwner_Id(50L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibraryStockReturnRequest(
                5L, "  sobrante  ", List.of(new LibraryStockReturnLineItemRequest(10L, 4)));

        var detail = service.registerLibraryStockReturn(owner, req);

        assertThat(detail.totalUnits()).isEqualTo(4);
        assertThat(detail.campaignName()).isEqualTo("Verano");
        verify(movementLogger)
                .log(
                        eq(owner),
                        eq(book),
                        eq(InventoryMovementType.LIBRARY_STOCK_RETURN),
                        anyInt(),
                        anyInt(),
                        eq("LIBRARY_STOCK_RETURN"),
                        eq(50L),
                        eq("sobrante"),
                        any(Instant.class));
    }

    @Test
    void listLibraryStockReturns_mapea() {
        var campaign = UnitTestFixtures.campaign(5L, "Verano");
        var ret = new LibraryStockReturn(owner, campaign, Instant.now(), "n");
        UnitTestFixtures.setId(ret, 7L);
        var book = UnitTestFixtures.book(1L, UnitTestFixtures.category(1L, "C"), "B", BigDecimal.ONE);
        var invLine = new LibrarySupplyInvoiceLine(null, book, 3, BigDecimal.TEN);
        ret.addLine(invLine, book, 3);
        when(libraryStockReturns.findByOwner_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(ret));

        var list = service.listLibraryStockReturns(owner);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().totalUnits()).isEqualTo(3);
        assertThat(list.getFirst().campaignName()).isEqualTo("Verano");
    }

    @Test
    void registerLibraryStockReturn_sinCampaña_notaNull() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        var invLine = new LibrarySupplyInvoiceLine(null, book, 10, new BigDecimal("100"));
        AtomicReference<LibraryStockReturn> saved = new AtomicReference<>();

        when(providerStock.allocateLibraryReturn(owner, 10L, 1))
                .thenReturn(List.of(new InvoiceLineAllocation(invLine, 1)));
        when(libraryStockReturns.save(any(LibraryStockReturn.class))).thenAnswer(inv -> {
            LibraryStockReturn r = inv.getArgument(0);
            UnitTestFixtures.setId(r, 61L);
            saved.set(r);
            return r;
        });
        when(libraryStockReturns.findDetailedByIdAndOwner_Id(61L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibraryStockReturnRequest(
                null, null, List.of(new LibraryStockReturnLineItemRequest(10L, 1)));

        assertThat(service.registerLibraryStockReturn(owner, req).note()).isNull();
    }

    @Test
    void registerLibraryStockReturn_sinCampaña_notaVacia() {
        var book = UnitTestFixtures.book(10L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("10"));
        var invLine = new LibrarySupplyInvoiceLine(null, book, 10, new BigDecimal("100"));
        AtomicReference<LibraryStockReturn> saved = new AtomicReference<>();

        when(providerStock.allocateLibraryReturn(owner, 10L, 2))
                .thenReturn(List.of(new InvoiceLineAllocation(invLine, 2)));
        when(libraryStockReturns.save(any(LibraryStockReturn.class))).thenAnswer(inv -> {
            LibraryStockReturn r = inv.getArgument(0);
            UnitTestFixtures.setId(r, 60L);
            saved.set(r);
            return r;
        });
        when(libraryStockReturns.findDetailedByIdAndOwner_Id(60L, 1L)).thenAnswer(inv -> Optional.of(saved.get()));

        var req = new CreateLibraryStockReturnRequest(
                null, "   ", List.of(new LibraryStockReturnLineItemRequest(10L, 2)));

        var detail = service.registerLibraryStockReturn(owner, req);

        assertThat(detail.campaignId()).isNull();
        assertThat(detail.note()).isNull();
    }

    @Test
    void registerLibraryStockReturn_campanaInexistente() {
        when(campaigns.findById(99L)).thenReturn(Optional.empty());
        var req = new CreateLibraryStockReturnRequest(
                99L, null, List.of(new LibraryStockReturnLineItemRequest(10L, 1)));
        assertThatThrownBy(() -> service.registerLibraryStockReturn(owner, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaña");
    }

    @Test
    void getLibraryStockReturn_ok() {
        var ret = new LibraryStockReturn(owner, null, Instant.now(), "det");
        UnitTestFixtures.setId(ret, 15L);
        when(libraryStockReturns.findDetailedByIdAndOwner_Id(15L, 1L)).thenReturn(Optional.of(ret));

        assertThat(service.getLibraryStockReturn(owner, 15L).id()).isEqualTo(15L);
    }

    @Test
    void listLibraryStockReturns_sinCampaña() {
        var ret = new LibraryStockReturn(owner, null, Instant.now(), "n");
        UnitTestFixtures.setId(ret, 8L);
        when(libraryStockReturns.findByOwner_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(ret));

        var list = service.listLibraryStockReturns(owner);

        assertThat(list.getFirst().campaignId()).isNull();
        assertThat(list.getFirst().campaignName()).isNull();
    }

    @Test
    void getLibraryStockReturn_noEncontrada() {
        when(libraryStockReturns.findDetailedByIdAndOwner_Id(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLibraryStockReturn(owner, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
