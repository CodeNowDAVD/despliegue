package biblioteca.gorbits.unit.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.inventory.InventoryMovement;
import biblioteca.gorbits.inventory.InventoryMovementType;
import biblioteca.gorbits.inventory.LibraryPayment;
import biblioteca.gorbits.inventory.LibraryStockReturn;
import biblioteca.gorbits.inventory.LibraryStockReturnLine;
import biblioteca.gorbits.inventory.LibrarySupplyInvoice;
import biblioteca.gorbits.inventory.LibrarySupplyInvoiceLine;
import biblioteca.gorbits.inventory.ProviderFieldStock;
import biblioteca.gorbits.inventory.StockWithdrawal;
import biblioteca.gorbits.inventory.StockWithdrawalLine;
import biblioteca.gorbits.inventory.WarehouseStock;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Cubre getters/setters y constructores de entidades de inventario. */
class InventoryEntityUnitTest {

    @Test
    void warehouseStock_gettersYSetters() {
        var book = UnitTestFixtures.book(1L, UnitTestFixtures.category(1L, "C"), "Lib", BigDecimal.ONE);
        var ws = new WarehouseStock(book, 10);
        UnitTestFixtures.setId(ws, 5L);

        assertThat(ws.getId()).isEqualTo(5L);
        assertThat(ws.getBook().getId()).isEqualTo(1L);
        assertThat(ws.getQuantity()).isEqualTo(10);
        ws.setQuantity(15);
        assertThat(ws.getQuantity()).isEqualTo(15);
    }

    @Test
    void stockWithdrawal_agregaLineas() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(2L, UnitTestFixtures.category(1L, "C"), "B", BigDecimal.ONE);
        var w = new StockWithdrawal(owner, Instant.parse("2026-03-01T12:00:00Z"), "nota");
        w.addLine(book, 3);

        assertThat(w.getOwner().getId()).isEqualTo(1L);
        assertThat(w.getNote()).isEqualTo("nota");
        assertThat(w.getLines()).hasSize(1);
        StockWithdrawalLine line = w.getLines().getFirst();
        UnitTestFixtures.setId(line, 77L);
        assertThat(line.getId()).isEqualTo(77L);
        assertThat(line.getBook().getId()).isEqualTo(2L);
        assertThat(line.getQuantity()).isEqualTo(3);
        assertThat(line.getWithdrawal()).isSameAs(w);
    }

    @Test
    void libraryPayment_getOwner() {
        var owner = UnitTestFixtures.proveedor(1L);
        var campaign = UnitTestFixtures.campaign(2L, "Camp");
        var payment = new LibraryPayment(
                owner,
                campaign,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 3, 15),
                "abono",
                Instant.parse("2026-03-15T10:00:00Z"));

        assertThat(payment.getOwner()).isSameAs(owner);
        assertThat(payment.getCampaign()).isSameAs(campaign);
        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");
        assertThat(payment.getPaidOn()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(payment.getNote()).isEqualTo("abono");
    }

    @Test
    void providerFieldStock_getters() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(3L, UnitTestFixtures.category(1L, "C"), "F", BigDecimal.ONE);
        var fs = new ProviderFieldStock(owner, book, 8);
        UnitTestFixtures.setId(fs, 6L);

        assertThat(fs.getId()).isEqualTo(6L);
        assertThat(fs.getOwner().getId()).isEqualTo(1L);
        assertThat(fs.getBook().getId()).isEqualTo(3L);
        assertThat(fs.getQuantity()).isEqualTo(8);
        fs.addQuantity(4);
        assertThat(fs.getQuantity()).isEqualTo(12);
    }

    @Test
    void inventoryMovement_getters() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(4L, UnitTestFixtures.category(1L, "C"), "Mov", BigDecimal.ONE);
        var at = Instant.parse("2026-03-10T09:00:00Z");
        var movement = new InventoryMovement(
                owner,
                book,
                InventoryMovementType.LIBRARY_INVOICE_ENTRY,
                5,
                3,
                2,
                "INVOICE",
                99L,
                "nota",
                at);
        UnitTestFixtures.setId(movement, 20L);

        assertThat(movement.getId()).isEqualTo(20L);
        assertThat(movement.getOwner()).isSameAs(owner);
        assertThat(movement.getBook()).isSameAs(book);
        assertThat(movement.getMovementType()).isEqualTo(InventoryMovementType.LIBRARY_INVOICE_ENTRY);
        assertThat(movement.getQuantityDelta()).isEqualTo(5);
        assertThat(movement.getWarehouseDelta()).isEqualTo(3);
        assertThat(movement.getFieldDelta()).isEqualTo(2);
        assertThat(movement.getReferenceType()).isEqualTo("INVOICE");
        assertThat(movement.getReferenceId()).isEqualTo(99L);
        assertThat(movement.getNote()).isEqualTo("nota");
        assertThat(movement.getOccurredAt()).isEqualTo(at);
    }

    @Test
    void librarySupplyInvoiceLine_getters() {
        var owner = UnitTestFixtures.proveedor(1L);
        var book = UnitTestFixtures.book(5L, UnitTestFixtures.category(1L, "C"), "Lib", new BigDecimal("25.00"));
        var invoice = new LibrarySupplyInvoice(
                owner,
                "FAC-001",
                LocalDate.of(2026, 4, 1),
                "nota",
                Instant.parse("2026-04-01T10:00:00Z"));
        var line = new LibrarySupplyInvoiceLine(invoice, book, 3, new BigDecimal("75.00"));
        UnitTestFixtures.setId(line, 42L);

        assertThat(line.getId()).isEqualTo(42L);
        assertThat(line.getInvoice()).isSameAs(invoice);
        assertThat(line.getBook()).isSameAs(book);
        assertThat(line.getQuantity()).isEqualTo(3);
        assertThat(line.getLineTotal()).isEqualByComparingTo("75.00");
    }

    @Test
    void libraryStockReturn_getOwnerYLíneas() {
        var owner = UnitTestFixtures.proveedor(1L);
        var campaign = UnitTestFixtures.campaign(2L, "Camp");
        var at = Instant.parse("2026-04-01T12:00:00Z");
        var ret = new LibraryStockReturn(owner, campaign, at, "devolución");
        var book = UnitTestFixtures.book(5L, UnitTestFixtures.category(1L, "C"), "Lib", BigDecimal.TEN);
        var invoice = new LibrarySupplyInvoice(
                owner, "FAC-002", LocalDate.of(2026, 3, 20), null, at);
        var invLine = new LibrarySupplyInvoiceLine(invoice, book, 2, BigDecimal.TEN);
        ret.addLine(invLine, book, 2);

        assertThat(ret.getOwner()).isSameAs(owner);
        assertThat(ret.getCampaign()).isSameAs(campaign);
        assertThat(ret.getCreatedAt()).isEqualTo(at);
        assertThat(ret.getNote()).isEqualTo("devolución");
        assertThat(ret.getLines()).hasSize(1);
        LibraryStockReturnLine returnLine = ret.getLines().getFirst();
        assertThat(returnLine.getQuantity()).isEqualTo(2);
        assertThat(returnLine.getBook()).isSameAs(book);
        assertThat(returnLine.getInvoiceLine()).isSameAs(invLine);
    }
}
