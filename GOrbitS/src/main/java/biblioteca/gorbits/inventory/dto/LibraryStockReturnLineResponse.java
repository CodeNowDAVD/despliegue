package biblioteca.gorbits.inventory.dto;

public record LibraryStockReturnLineResponse(
        Long invoiceLineId, Long bookId, String bookTitle, int quantity) {}
