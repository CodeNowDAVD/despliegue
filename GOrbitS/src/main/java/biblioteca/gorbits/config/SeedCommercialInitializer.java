package biblioteca.gorbits.config;

import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.CampaignRepository;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.ProviderProfileRepository;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.commercial.SalesZoneRepository;
import biblioteca.gorbits.user.UserAccountRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"h2", "test"})
@Order(300)
public class SeedCommercialInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedCommercialInitializer.class);

    private static final String MARKER_CAMPAIGN_NAME = "Campaña 2026";

    private final SalesZoneRepository zones;
    private final CampaignRepository campaigns;
    private final ProviderProfileRepository profiles;
    private final UserAccountRepository users;

    public SeedCommercialInitializer(
            SalesZoneRepository zones,
            CampaignRepository campaigns,
            ProviderProfileRepository profiles,
            UserAccountRepository users) {
        this.zones = zones;
        this.campaigns = campaigns;
        this.profiles = profiles;
        this.users = users;
    }

    @Override
    public void run(String... args) {
        ensureZone("Campo B");
        SalesZone campoA = ensureZone("Campo A");

        boolean hasMarkerCampaign = campaigns.findAllByOrderByStartsOnDesc().stream()
                .anyMatch(c -> MARKER_CAMPAIGN_NAME.equals(c.getName()));
        if (!hasMarkerCampaign) {
            campaigns.save(new Campaign(
                    MARKER_CAMPAIGN_NAME, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        }

        users.findByUsername("proveedor").ifPresent(u -> {
            if (profiles.findWithZoneByUser_Id(u.getId()).isEmpty()) {
                ProviderProfile profile = new ProviderProfile(u, campoA);
                profile.setPersonalData(
                        "María",
                        "González",
                        "12345678",
                        "70000001",
                        "maria.gonzalez@ejemplo.com",
                        "Administración de empresas");
                profiles.save(profile);
            }
        });
        log.info("Zonas, campaña y perfil de proveedor sembrados.");
    }

    private SalesZone ensureZone(String name) {
        return zones.findAll().stream()
                .filter(z -> name.equalsIgnoreCase(z.getName()))
                .findFirst()
                .orElseGet(() -> zones.save(new SalesZone(name)));
    }
}
