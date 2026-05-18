package biblioteca.gorbits.inventory.dto;

public record ProviderBookStockRowResponse(
        Long bookId,
        String bookTitle,
        Long categoryId,
        String categoryName,
        int purchasedFromLibrary,
        int returnedToLibrary,
        int soldOnContracts,
        int available) {}
