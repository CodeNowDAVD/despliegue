package biblioteca.gorbits.admin.web;

import biblioteca.gorbits.admin.AdminUsersService;
import biblioteca.gorbits.admin.dto.AdminCreateProviderRequest;
import biblioteca.gorbits.admin.dto.AdminResetPasswordRequest;
import biblioteca.gorbits.admin.dto.AdminSetEnabledRequest;
import biblioteca.gorbits.admin.dto.AdminSetRoleRequest;
import biblioteca.gorbits.admin.dto.UserAccountSummaryResponse;
import biblioteca.gorbits.commercial.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final AdminUsersService adminUsersService;
    private final CurrentUserService currentUser;

    public AdminUsersController(AdminUsersService adminUsersService, CurrentUserService currentUser) {
        this.adminUsersService = adminUsersService;
        this.currentUser = currentUser;
    }

    /** Listado para gestión: contraseñas olvidadas, bloqueos, altas. Sin contraseñas en respuesta. */
    @GetMapping
    public List<UserAccountSummaryResponse> list() {
        return adminUsersService.listUsers();
    }

    /** Asigna contraseña nueva a cualquier usuario (olvido de contraseña sin flujo de email). */
    @PutMapping("/{id}/password")
    public UserAccountSummaryResponse resetPassword(
            @PathVariable long id, @RequestBody @Valid AdminResetPasswordRequest request) {
        return adminUsersService.resetPassword(id, request.newPassword());
    }

    @PatchMapping("/{id}/enabled")
    public UserAccountSummaryResponse setEnabled(
            @PathVariable long id, @RequestBody @Valid AdminSetEnabledRequest request) {
        return adminUsersService.setEnabled(id, request.enabled(), currentUser.currentUser().getId());
    }

    @PatchMapping("/{id}/role")
    public UserAccountSummaryResponse setRole(
            @PathVariable long id, @RequestBody @Valid AdminSetRoleRequest request) {
        return adminUsersService.updateRole(id, request.role(), currentUser.currentUser().getId());
    }

    /** Crea un usuario proveedor y su perfil con la primera zona del sistema como predeterminada. */
    @PostMapping("/providers")
    public UserAccountSummaryResponse createProvider(@RequestBody @Valid AdminCreateProviderRequest request) {
        return adminUsersService.createProvider(request);
    }
}
