package biblioteca.gorbits.admin.dto;

import biblioteca.gorbits.user.Role;

public record UserAccountSummaryResponse(long id, String username, Role role, boolean enabled) {}
