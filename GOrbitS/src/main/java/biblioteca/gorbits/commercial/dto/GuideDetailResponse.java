package biblioteca.gorbits.commercial.dto;

import biblioteca.gorbits.commercial.GuideStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GuideDetailResponse(
        Long id,
        String contractNumber,
        LocalDate orderDate,
        Long campaignId,
        String campaignName,
        Long clientId,
        String clientName,
        GuideStatus status,
        Instant createdAt,
        String note,
        List<GuideLineResponse> lines,
        BigDecimal totalAmount,
        List<String> tags,
        ClientReturnInfoResponse clientReturn
) {
}
