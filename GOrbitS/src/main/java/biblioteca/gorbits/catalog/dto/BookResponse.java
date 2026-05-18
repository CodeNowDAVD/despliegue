package biblioteca.gorbits.catalog.dto;

import biblioteca.gorbits.catalog.BookType;
import java.math.BigDecimal;

public record BookResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String title,
        BigDecimal price,
        BookType bookType,
        String packageNote,
        Long companionBookId,
        String companionBookTitle,
        BigDecimal companionLinePrice) {}
