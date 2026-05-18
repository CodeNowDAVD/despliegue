package biblioteca.gorbits.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateLibraryStockReturnRequest(
        Long campaignId, String note, @NotEmpty @Valid List<LibraryStockReturnLineItemRequest> lines) {}
