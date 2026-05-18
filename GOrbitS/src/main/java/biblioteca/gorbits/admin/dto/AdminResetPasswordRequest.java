package biblioteca.gorbits.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contraseña nueva asignada por un administrador (p. ej. usuario olvidó la suya).
 * El admin debe comunicarla por un canal fiable fuera de la aplicación.
 */
public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 128) String newPassword) {}
