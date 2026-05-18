package biblioteca.gorbits.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateProviderRequest(
        @NotBlank @Size(min = 2, max = 80) String username,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 20) String dni,
        @NotBlank @Size(max = 40) String phone,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 120) String career) {}
