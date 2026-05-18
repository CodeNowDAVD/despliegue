package biblioteca.gorbits.inventory.dto;

import java.time.Instant;
import java.util.List;

public record WithdrawalDetailResponse(
        Long id, Instant createdAt, String note, int totalUnits, List<WithdrawalLineResponse> lines) {}
