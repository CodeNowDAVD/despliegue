package biblioteca.gorbits.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LibrarySupplyInvoiceDetailResponse(
        Long id,
        Long ownerId,
        String ownerUsername,
        String invoiceNumber,
        LocalDate issuedOn,
        String note,
        int totalUnits,
        BigDecimal totalAmount,
        Instant createdAt,
        List<LibrarySupplyLineResponse> lines) {}
