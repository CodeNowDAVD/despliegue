package biblioteca.gorbits.inventory.dto;

import java.util.List;

public record ProviderBookStockByCategoryResponse(
        Long categoryId, String categoryName, int totalAvailable, List<ProviderBookStockRowResponse> books) {}
