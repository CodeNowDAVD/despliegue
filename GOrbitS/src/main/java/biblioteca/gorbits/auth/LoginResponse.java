package biblioteca.gorbits.auth;

import biblioteca.gorbits.user.Role;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String username,
        Role role
) {
}
