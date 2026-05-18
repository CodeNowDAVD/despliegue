package biblioteca.gorbits.catalog.dto;

import biblioteca.gorbits.catalog.BookType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 200) String title,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal price,
        @NotNull BookType bookType,
        @Size(max = 500) String packageNote,
        Long companionBookId,
        @DecimalMin(value = "0.00", inclusive = true) BigDecimal companionLinePrice) {}
