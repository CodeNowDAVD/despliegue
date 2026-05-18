package biblioteca.gorbits.inventory.dto;

import jakarta.validation.constraints.Positive;

public record WithdrawalLineRequest(Long bookId, @Positive int quantity) {}
