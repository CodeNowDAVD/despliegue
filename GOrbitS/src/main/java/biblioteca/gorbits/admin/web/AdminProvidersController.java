package biblioteca.gorbits.admin.web;

import biblioteca.gorbits.admin.AdminProvidersService;
import biblioteca.gorbits.admin.dto.AdminUpdateProviderRequest;
import biblioteca.gorbits.admin.dto.ProviderAccountResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/providers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProvidersController {

    private final AdminProvidersService adminProvidersService;

    public AdminProvidersController(AdminProvidersService adminProvidersService) {
        this.adminProvidersService = adminProvidersService;
    }

    @GetMapping
    public List<ProviderAccountResponse> list() {
        return adminProvidersService.listProveedores();
    }

    @PutMapping("/{userId}")
    public ProviderAccountResponse update(
            @PathVariable long userId, @RequestBody @Valid AdminUpdateProviderRequest request) {
        return adminProvidersService.updateProveedor(userId, request);
    }
}
