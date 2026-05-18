package biblioteca.gorbits.admin;

import biblioteca.gorbits.admin.dto.AdminCreateProviderRequest;
import biblioteca.gorbits.admin.dto.UserAccountSummaryResponse;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUsersService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ProviderProfileRepository providerProfiles;
    private final SalesZoneRepository zones;

    public AdminUsersService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            ProviderProfileRepository providerProfiles,
            SalesZoneRepository zones) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.providerProfiles = providerProfiles;
        this.zones = zones;
    }

    @Transactional(readOnly = true)
    public List<UserAccountSummaryResponse> listUsers() {
        return users.findAllByOrderByUsernameAsc().stream().map(AdminUsersService::toSummary).toList();
    }

    @Transactional
    public UserAccountSummaryResponse resetPassword(long targetUserId, String newPassword) {
        UserAccount u =
                users.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        String trimmed = newPassword.trim();
        if (trimmed.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        u.setPasswordHash(passwordEncoder.encode(trimmed));
        users.save(u);
        return toSummary(u);
    }

    @Transactional
    public UserAccountSummaryResponse setEnabled(long targetUserId, boolean enabled, long adminUserId) {
        if (targetUserId == adminUserId && !enabled) {
            throw new IllegalArgumentException("No puede deshabilitar su propia cuenta de administrador");
        }
        UserAccount u =
                users.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        u.setEnabled(enabled);
        users.save(u);
        return toSummary(u);
    }

    @Transactional
    public UserAccountSummaryResponse updateRole(long targetUserId, Role newRole, long adminUserId) {
        if (targetUserId == adminUserId) {
            throw new IllegalArgumentException("No puede cambiar su propio rol");
        }
        UserAccount u =
                users.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (u.getRole() == newRole) {
            return toSummary(u);
        }
        if (u.getRole() == Role.ADMIN && newRole == Role.PROVEEDOR && users.countByRole(Role.ADMIN) <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador en el sistema");
        }
        u.setRole(newRole);
        users.save(u);
        if (newRole == Role.PROVEEDOR) {
            ensureProviderProfile(u);
        }
        return toSummary(u);
    }

    @Transactional
    public UserAccountSummaryResponse createProvider(AdminCreateProviderRequest request) {
        String username = request.username().trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre");
        }
        String dni = request.dni().trim();
        if (providerProfiles.existsByDni(dni)) {
            throw new IllegalArgumentException("Ya existe un embajador con ese DNI");
        }
        UserAccount u = new UserAccount(
                username, passwordEncoder.encode(request.password().trim()), Role.PROVEEDOR, true);
        u = users.save(u);
        SalesZone defaultZone = zones
                .findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No hay zonas comerciales en el sistema"));
        ProviderProfile profile = new ProviderProfile(u, defaultZone);
        profile.setPersonalData(
                request.firstName().trim(),
                request.lastName().trim(),
                dni,
                request.phone().trim(),
                request.email().trim(),
                request.career().trim());
        providerProfiles.save(profile);
        return toSummary(u);
    }

    private void ensureProviderProfile(UserAccount user) {
        if (!providerProfiles.existsByUser_Id(user.getId())) {
            SalesZone defaultZone = zones
                    .findFirstByOrderByIdAsc()
                    .orElseThrow(() -> new IllegalStateException("No hay zonas comerciales en el sistema"));
            providerProfiles.save(new ProviderProfile(user, defaultZone));
        }
    }

    private static UserAccountSummaryResponse toSummary(UserAccount u) {
        return new UserAccountSummaryResponse(u.getId(), u.getUsername(), u.getRole(), u.isEnabled());
    }
}
