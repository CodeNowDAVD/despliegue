package biblioteca.gorbits.inventory.dto;

import java.time.Instant;
import java.util.List;

public record LibraryStockReturnDetailResponse(
        Long id,
        Instant createdAt,
        String note,
        int totalUnits,
        Long campaignId,
        String campaignName,
        List<LibraryStockReturnLineResponse> lines) {}
