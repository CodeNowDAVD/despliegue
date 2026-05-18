package biblioteca.gorbits.commercial.dto;

import biblioteca.gorbits.catalog.BookType;
import java.math.BigDecimal;

public record GuideLineResponse(
        Long id,
        Long bookId,
        String bookTitle,
        BookType bookType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
