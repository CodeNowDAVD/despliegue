package biblioteca.gorbits.commercial.dto;

import biblioteca.gorbits.commercial.GuideStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GuideListItemResponse(
        Long id,
        String contractNumber,
        LocalDate orderDate,
        Long campaignId,
        String campaignName,
        Long clientId,
        String clientName,
        GuideStatus status,
        Instant createdAt,
        List<String> tags
) {
}
