package biblioteca.gorbits.unit.commercial;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.Client;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.SalesContractTag;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesGuideLine;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Cubre getters/setters y ramas de entidades del paquete commercial. */
class CommercialEntityUnitTest {

    @Test
    void campaign_setters() {
        var campaign = new Campaign("Vieja", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        UnitTestFixtures.setId(campaign, 1L);

        campaign.setName("Nueva");
        campaign.setStartsOn(LocalDate.of(2026, 2, 1));
        campaign.setEndsOn(LocalDate.of(2026, 12, 31));

        assertThat(campaign.getName()).isEqualTo("Nueva");
        assertThat(campaign.getStartsOn()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(campaign.getEndsOn()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void salesZone_setName() {
        var zone = UnitTestFixtures.zone(1L, "Norte");

        zone.setName("Sur");

        assertThat(zone.getName()).isEqualTo("Sur");
    }

    @Test
    void salesContractTag_setName() {
        var owner = UnitTestFixtures.proveedor(1L);
        var tag = new SalesContractTag(owner, "VIP");

        tag.setName("Premium");

        assertThat(tag.getName()).isEqualTo("Premium");
    }

    @Test
    void salesGuideLine_getGuide() {
        var owner = UnitTestFixtures.proveedor(1L);
        var guide = UnitTestFixtures.guide(
                1L, owner, UnitTestFixtures.campaign(1L, "C"), UnitTestFixtures.client(1L, owner, "Cli"), GuideStatus.ACTIVA);
        var book = UnitTestFixtures.book(3L, UnitTestFixtures.category(1L, "Cat"), "Libro", BigDecimal.TEN);
        guide.addLine(book, 2, BigDecimal.TEN);

        SalesGuideLine line = guide.getLines().getFirst();

        assertThat(line.getGuide()).isSameAs(guide);
        assertThat(line.getBook()).isSameAs(book);
        assertThat(line.getQuantity()).isEqualTo(2);
    }

    @Test
    void client_gettersYSetters() {
        var owner = UnitTestFixtures.proveedor(1L);
        var client = new Client(owner, "Nombre", "111", "e@mail.com", "dir");
        UnitTestFixtures.setId(client, 4L);

        assertThat(client.getOwner()).isSameAs(owner);
        assertThat(client.getEmail()).isEqualTo("e@mail.com");
        client.setEmail("nuevo@mail.com");
        assertThat(client.getEmail()).isEqualTo("nuevo@mail.com");
    }

    @Test
    void providerProfile_getId() {
        var profile = UnitTestFixtures.providerProfile(7L, UnitTestFixtures.proveedor(1L), null);

        assertThat(profile.getId()).isEqualTo(7L);
    }
}
