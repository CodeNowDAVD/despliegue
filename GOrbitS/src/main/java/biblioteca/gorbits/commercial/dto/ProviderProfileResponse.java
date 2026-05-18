package biblioteca.gorbits.commercial.dto;

/** Perfil del embajador (proveedor): datos personales de solo lectura + zona asignada. */
public record ProviderProfileResponse(
        String username,
        String firstName,
        String lastName,
        String dni,
        String phone,
        String email,
        String career,
        Long zoneId,
        String zoneName) {}
