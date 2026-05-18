package biblioteca.gorbits.inventory.dto;

import java.time.Instant;

public record WithdrawalListItemResponse(Long id, Instant createdAt, String note, int totalUnits) {}
