package biblioteca.gorbits.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSalesContractTagRequest(@NotBlank @Size(max = 80) String name) {}
