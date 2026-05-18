package biblioteca.gorbits.commercial;

import biblioteca.gorbits.admin.dto.ProviderAccountResponse;
import biblioteca.gorbits.commercial.dto.ProviderProfileResponse;
import biblioteca.gorbits.user.UserAccount;

public final class EmbassadorProfileMapper {

    private EmbassadorProfileMapper() {}

    public static ProviderProfileResponse toResponse(ProviderProfile profile, UserAccount user) {
        SalesZone z = profile.getZone();
        return new ProviderProfileResponse(
                user.getUsername(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDni(),
                profile.getPhone(),
                profile.getEmail(),
                profile.getCareer(),
                z != null ? z.getId() : null,
                z != null ? z.getName() : null);
    }

    public static ProviderAccountResponse toAdminListItem(ProviderProfile profile) {
        UserAccount u = profile.getUser();
        SalesZone z = profile.getZone();
        return new ProviderAccountResponse(
                u.getId(),
                u.getUsername(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDni(),
                profile.getPhone(),
                profile.getEmail(),
                profile.getCareer(),
                z != null ? z.getId() : null,
                z != null ? z.getName() : null);
    }
}
