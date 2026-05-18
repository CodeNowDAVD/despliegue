package biblioteca.gorbits.commercial.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProviderProfileRequest(@NotNull Long zoneId) {
}
