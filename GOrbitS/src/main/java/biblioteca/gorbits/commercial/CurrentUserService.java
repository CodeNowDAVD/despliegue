package biblioteca.gorbits.commercial;

import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return users.findByUsername(a.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    public UserAccount requireProveedor() {
        UserAccount u = currentUser();
        if (u.getRole() != Role.PROVEEDOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo disponible para proveedores");
        }
        return u;
    }
}
