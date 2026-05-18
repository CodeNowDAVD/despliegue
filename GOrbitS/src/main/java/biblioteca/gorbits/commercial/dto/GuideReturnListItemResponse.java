package biblioteca.gorbits.commercial.dto;

import java.time.Instant;

public record GuideReturnListItemResponse(
        Long guideId,
        Long campaignId,
        String campaignName,
        Long clientId,
        String clientName,
        Instant returnedAt,
        String reason,
        boolean hiddenFromReturnList) {}
