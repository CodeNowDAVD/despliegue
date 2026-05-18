package biblioteca.gorbits.inventory.dto;

import java.math.BigDecimal;

public record LibrarySupplyLineResponse(
        Long id,
        Long bookId,
        String title,
        int invoicedQuantity,
        int returnedQuantity,
        int netQuantity,
        BigDecimal lineTotal) {}
