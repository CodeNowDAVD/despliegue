package biblioteca.gorbits.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resumen: unidades e importes (facturas de compra vs depósitos al proveedor de librería). */
public record LibraryReconciliationSummaryResponse(
        Long campaignId,
        String campaignName,
        LocalDate periodFrom,
        LocalDate periodTo,
        long purchasedFromLibraryUnits,
        long returnedToLibraryUnits,
        long netPurchasedUnits,
        long unitsInClosedGuides,
        BigDecimal totalInvoicedAmount,
        BigDecimal totalDepositsToLibrary,
        BigDecimal netBalanceOwedToLibrary) {}
