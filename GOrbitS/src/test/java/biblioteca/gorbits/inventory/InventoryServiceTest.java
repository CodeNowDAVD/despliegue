package biblioteca.gorbits.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuideRepository;
import biblioteca.gorbits.inventory.dto.CreateLibraryPaymentRequest;
import biblioteca.gorbits.inventory.dto.SetWarehouseStockRequest;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

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
    void libraryReconciliationSummary_sinCampaña() {
        when(libraryInvoices.sumPurchasedUnitsByOwner(1L)).thenReturn(100L);
        when(libraryStockReturns.sumLineQtyByOwner(1L)).thenReturn(10L);
        when(salesGuides.sumLineQtyForOwnerAndStatus(1L, GuideStatus.CERRADA)).thenReturn(40L);
        when(libraryInvoices.sumAllLineAmountsByOwner(1L)).thenReturn(new BigDecimal("5000.00"));
        when(libraryPayments.sumAmountByOwner(1L)).thenReturn(new BigDecimal("3000.00"));

        var summary = service.libraryReconciliationSummary(owner, null);

        assertThat(summary.campaignId()).isNull();
        assertThat(summary.netPurchasedUnits()).isEqualTo(90L);
        assertThat(summary.unitsInClosedGuides()).isEqualTo(40L);
        assertThat(summary.netBalanceOwedToLibrary()).isEqualByComparingTo("2000.00");
    }

    @Test
    void libraryReconciliationSummary_porCampaña() {
        var campaign = UnitTestFixtures.campaign(5L, "Verano");
        when(campaigns.findById(5L)).thenReturn(Optional.of(campaign));
        when(libraryInvoices.sumLineQtyIssuedBetweenForOwner(1L, campaign.getStartsOn(), campaign.getEndsOn()))
                .thenReturn(20L);
        when(libraryStockReturns.sumLineQtyByOwnerAndCampaign(1L, 5L)).thenReturn(2L);
        when(salesGuides.sumLineQtyForOwnerCampaignAndStatus(1L, 5L, GuideStatus.CERRADA))
                .thenReturn(8L);
        when(libraryInvoices.sumLineAmountIssuedBetweenForOwner(1L, campaign.getStartsOn(), campaign.getEndsOn()))
                .thenReturn(new BigDecimal("800.00"));
        when(libraryPayments.sumAmountByOwnerAndCampaign(1L, 5L)).thenReturn(new BigDecimal("600.00"));

        var summary = service.libraryReconciliationSummary(owner, 5L);

        assertThat(summary.campaignName()).isEqualTo("Verano");
        assertThat(summary.netPurchasedUnits()).isEqualTo(18L);
        assertThat(summary.netBalanceOwedToLibrary()).isEqualByComparingTo("200.00");
    }

    @Test
    void libraryReconciliationSummary_campanaInexistente() {
        when(campaigns.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.libraryReconciliationSummary(owner, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setWarehouseStock_creaSiNoExiste() {
        var cat = UnitTestFixtures.category(1L, "C");
        var book = UnitTestFixtures.book(10L, cat, "Libro", new BigDecimal("1.00"));
        when(books.findById(10L)).thenReturn(Optional.of(book));
        when(warehouse.findByBook_Id(10L)).thenReturn(Optional.empty());
        when(warehouse.save(any(WarehouseStock.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setWarehouseStock(10L, new SetWarehouseStockRequest(25));

        verify(warehouse).save(any(WarehouseStock.class));
    }

    @Test
    void setWarehouseStock_libroInexistente() {
        when(books.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setWarehouseStock(99L, new SetWarehouseStockRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLibraryPayment_noEncontrado() {
        when(libraryPayments.findByIdAndOwner_Id(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLibraryPayment(owner, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerLibraryPayment_campanaInexistente() {
        when(campaigns.findById(99L)).thenReturn(Optional.empty());
        var req = new CreateLibraryPaymentRequest(new BigDecimal("10"), LocalDate.now(), null, 99L);
        assertThatThrownBy(() -> service.registerLibraryPayment(owner, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registerLibraryPayment_conCampañaYNotaVacia() {
        var campaign = UnitTestFixtures.campaign(5L, "Verano");
        when(campaigns.findById(5L)).thenReturn(Optional.of(campaign));
        when(libraryPayments.save(any(LibraryPayment.class))).thenAnswer(inv -> {
            LibraryPayment p = inv.getArgument(0);
            UnitTestFixtures.setId(p, 2L);
            return p;
        });
        when(libraryPayments.findByIdAndOwner_Id(2L, 1L)).thenAnswer(inv -> {
            LibraryPayment p = new LibraryPayment(owner, campaign, new BigDecimal("50.00"), LocalDate.now(), null, java.time.Instant.now());
            UnitTestFixtures.setId(p, 2L);
            return Optional.of(p);
        });

        var req = new CreateLibraryPaymentRequest(new BigDecimal("50.00"), LocalDate.now(), "  ", 5L);
        var detail = service.registerLibraryPayment(owner, req);

        assertThat(detail.campaignName()).isEqualTo("Verano");
        assertThat(detail.note()).isNull();
    }

    @Test
    void registerLibraryPayment_detalleNoEncontradoTrasGuardar() {
        when(libraryPayments.save(any(LibraryPayment.class))).thenAnswer(inv -> {
            LibraryPayment p = inv.getArgument(0);
            UnitTestFixtures.setId(p, 3L);
            return p;
        });
        when(libraryPayments.findByIdAndOwner_Id(3L, 1L)).thenReturn(Optional.empty());

        var req = new CreateLibraryPaymentRequest(new BigDecimal("10"), LocalDate.now(), null, null);
        assertThatThrownBy(() -> service.registerLibraryPayment(owner, req))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void listLibraryPayments_conYSinCampaña() {
        var campaign = UnitTestFixtures.campaign(5L, "Verano");
        var conCamp = new LibraryPayment(owner, campaign, new BigDecimal("10"), LocalDate.now(), "n", java.time.Instant.now());
        UnitTestFixtures.setId(conCamp, 1L);
        var sinCamp = new LibraryPayment(owner, null, new BigDecimal("5"), LocalDate.now(), null, java.time.Instant.now());
        UnitTestFixtures.setId(sinCamp, 2L);
        when(libraryPayments.findForOwnerOrderByPaidOnDesc(1L)).thenReturn(List.of(conCamp, sinCamp));

        var list = service.listLibraryPayments(owner);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).campaignName()).isEqualTo("Verano");
        assertThat(list.get(1).campaignId()).isNull();
    }

    @Test
    void scaleMoney_null_devuelveCero() {
        BigDecimal result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "scaleMoney", (BigDecimal) null);
        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void registerLibraryPayment_sinCampaña() {
        when(libraryPayments.save(any(LibraryPayment.class))).thenAnswer(inv -> {
            LibraryPayment p = inv.getArgument(0);
            UnitTestFixtures.setId(p, 1L);
            return p;
        });
        when(libraryPayments.findByIdAndOwner_Id(1L, 1L)).thenAnswer(inv -> {
            LibraryPayment p = new LibraryPayment(owner, null, new BigDecimal("150.50"), LocalDate.of(2026, 3, 10), "nota", java.time.Instant.now());
            UnitTestFixtures.setId(p, 1L);
            return Optional.of(p);
        });

        var req = new CreateLibraryPaymentRequest(new BigDecimal("150.50"), LocalDate.of(2026, 3, 10), "nota", null);
        var detail = service.registerLibraryPayment(owner, req);

        assertThat(detail.amount()).isEqualByComparingTo("150.50");
        assertThat(detail.campaignId()).isNull();
    }

    @Test
    void listFieldStock_mapeaFilas() {
        var book = UnitTestFixtures.book(1L, UnitTestFixtures.category(1L, "C"), "Libro", new BigDecimal("1"));
        var fs = new ProviderFieldStock(owner, book, 5);
        when(field.findPositiveByOwner_Id(1L)).thenReturn(List.of(fs));

        assertThat(service.listFieldStock(owner)).hasSize(1).first().satisfies(r -> {
            assertThat(r.bookId()).isEqualTo(1L);
            assertThat(r.quantity()).isEqualTo(5);
        });
    }

    @Test
    void registerWithdrawal_sinStockEnAlmacen_lanza() {
        var book = UnitTestFixtures.book(2L, UnitTestFixtures.category(1L, "C"), "X", new BigDecimal("1"));
        when(books.findById(2L)).thenReturn(Optional.of(book));
        when(warehouse.findForUpdateByBook_Id(2L)).thenReturn(Optional.empty());

        var req = new biblioteca.gorbits.inventory.dto.CreateWithdrawalRequest(
                null, List.of(new biblioteca.gorbits.inventory.dto.WithdrawalLineRequest(2L, 1)));
        assertThatThrownBy(() -> service.registerWithdrawal(owner, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("almacén");
    }

    @Test
    void listProviderBookStock_delegaEnProviderStock() {
        var row = new ProviderStockService.ProviderBookStockRow(1L, "A", 2L, "Cat", 10, 1, 3, 6);
        when(providerStock.stockByBook(owner)).thenReturn(List.of(row));

        assertThat(service.listProviderBookStock(owner)).hasSize(1).first().satisfies(r -> {
            assertThat(r.bookId()).isEqualTo(1L);
            assertThat(r.available()).isEqualTo(6);
        });
    }
}
