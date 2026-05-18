package biblioteca.gorbits.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminSetEnabledRequest(@NotNull Boolean enabled) {}
