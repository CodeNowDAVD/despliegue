package biblioteca.gorbits.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateWithdrawalRequest(String note, @NotEmpty @Valid List<WithdrawalLineRequest> lines) {}
