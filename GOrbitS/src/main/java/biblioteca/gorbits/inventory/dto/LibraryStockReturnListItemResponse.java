package biblioteca.gorbits.inventory.dto;

import java.time.Instant;

public record LibraryStockReturnListItemResponse(
        Long id,
        Instant createdAt,
        String note,
        int totalUnits,
        Long campaignId,
        String campaignName) {}
