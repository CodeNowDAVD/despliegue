package biblioteca.gorbits.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Devolución a librería por libro; el sistema asigna FIFO a líneas de factura. */
public record LibraryStockReturnLineItemRequest(@NotNull Long bookId, @Positive int quantity) {}
