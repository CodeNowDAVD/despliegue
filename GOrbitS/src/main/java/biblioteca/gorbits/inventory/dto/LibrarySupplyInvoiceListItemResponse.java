package biblioteca.gorbits.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record LibrarySupplyInvoiceListItemResponse(
        Long id,
        Long ownerId,
        String ownerUsername,
        String invoiceNumber,
        LocalDate issuedOn,
        int totalUnits,
        BigDecimal totalAmount,
        Instant createdAt) {}
