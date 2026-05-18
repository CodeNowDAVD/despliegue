package biblioteca.gorbits.commercial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 40) String phone,
        @Size(max = 120) String email,
        @Size(max = 500) String addressNote
) {
}
