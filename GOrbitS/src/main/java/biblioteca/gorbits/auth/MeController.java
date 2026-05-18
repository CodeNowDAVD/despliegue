package biblioteca.gorbits.auth;

import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserAccountRepository users;

    public MeController(UserAccountRepository users) {
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return users.findByUsername(authentication.getName())
                .map(u -> ResponseEntity.ok(new MeResponse(u.getId(), u.getUsername(), u.getRole())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record MeResponse(long id, String username, Role role) {
    }
}
