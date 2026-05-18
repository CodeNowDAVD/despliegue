package biblioteca.gorbits.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateLibrarySupplyInvoiceRequest(
        @NotBlank String invoiceNumber,
        @NotNull LocalDate issuedOn,
        String note,
        @NotEmpty @Valid List<LibrarySupplyLineItemRequest> lines,
        Long ownerUserId) {}
