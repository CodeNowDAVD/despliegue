package biblioteca.gorbits.config;

import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"h2", "test"})
@Order(100)
public class SeedUsersInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedUsersInitializer.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public SeedUsersInitializer(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureUser("admin", "admin123", Role.ADMIN);
        ensureUser("proveedor", "proveedor123", Role.PROVEEDOR);
    }

    private void ensureUser(String username, String plainPassword, Role role) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        users.save(new UserAccount(username, passwordEncoder.encode(plainPassword), role, true));
        log.info("Usuario sandbox creado: {} / {}", username, plainPassword);
    }
}
