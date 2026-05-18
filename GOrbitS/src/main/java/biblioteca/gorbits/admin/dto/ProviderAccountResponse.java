package biblioteca.gorbits.admin.dto;

/** Embajador (proveedor) con perfil completo para listado admin. */
public record ProviderAccountResponse(
        long id,
        String username,
        String firstName,
        String lastName,
        String dni,
        String phone,
        String email,
        String career,
        Long zoneId,
        String zoneName) {}
