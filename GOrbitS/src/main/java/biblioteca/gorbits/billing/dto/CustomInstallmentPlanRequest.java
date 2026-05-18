package biblioteca.gorbits.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CustomInstallmentPlanRequest(@NotEmpty @Valid List<CustomInstallmentItem> items) {
}
