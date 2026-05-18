package biblioteca.gorbits.admin;

import biblioteca.gorbits.admin.dto.AdminUpdateProviderRequest;
import biblioteca.gorbits.admin.dto.ProviderAccountResponse;
import biblioteca.gorbits.catalog.ResourceNotFoundException;
import biblioteca.gorbits.commercial.EmbassadorProfileMapper;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import biblioteca.gorbits.user.UserAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProvidersService {

    private final ProviderProfileRepository profiles;
    private final UserAccountRepository users;

    public AdminProvidersService(ProviderProfileRepository profiles, UserAccountRepository users) {
        this.profiles = profiles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProviderAccountResponse> listProveedores() {
        return profiles.findAllByUserRole(Role.PROVEEDOR).stream()
                .map(EmbassadorProfileMapper::toAdminListItem)
                .toList();
    }

    @Transactional
    public ProviderAccountResponse updateProveedor(long userId, AdminUpdateProviderRequest request) {
        ProviderProfile profile = profiles
                .findWithZoneByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Embajador no encontrado"));
        UserAccount user = profile.getUser();
        if (user.getRole() != Role.PROVEEDOR) {
            throw new IllegalArgumentException("El usuario no es un embajador");
        }
        String dni = request.dni().trim();
        if (profiles.existsByDniAndUser_IdNot(dni, userId)) {
            throw new IllegalArgumentException("Ya existe otro embajador con ese DNI");
        }
        String username = request.username().trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        if (users.existsByUsernameAndIdNot(username, userId)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre de acceso");
        }
        user.setUsername(username);
        users.save(user);
        profile.setPersonalData(
                request.firstName().trim(),
                request.lastName().trim(),
                dni,
                request.phone().trim(),
                request.email().trim(),
                request.career().trim());
        profiles.save(profile);
        return EmbassadorProfileMapper.toAdminListItem(profile);
    }
}
