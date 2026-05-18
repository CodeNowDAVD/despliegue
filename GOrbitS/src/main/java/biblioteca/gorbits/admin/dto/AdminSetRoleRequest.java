package biblioteca.gorbits.admin.dto;

import biblioteca.gorbits.user.Role;
import jakarta.validation.constraints.NotNull;

public record AdminSetRoleRequest(@NotNull Role role) {}
