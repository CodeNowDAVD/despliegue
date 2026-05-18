package biblioteca.gorbits.unit.commercial;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesContractTag;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Cobertura de {@link SalesGuide}: owner, nota y etiquetas. */
class SalesGuideUnitTest {

    @Test
    void getOwner_devuelveProveedorDelConstructor() {
        var owner = UnitTestFixtures.proveedor(1L);
        var guide = guide(owner);

        assertThat(guide.getOwner()).isSameAs(owner);
    }

    @Test
    void setNote_actualizaNota() {
        var guide = guide(UnitTestFixtures.proveedor(1L));

        guide.setNote("observación");

        assertThat(guide.getNote()).isEqualTo("observación");
    }

    @Test
    void setTags_reemplazaEtiquetas() {
        var owner = UnitTestFixtures.proveedor(1L);
        var guide = guide(owner);
        var tag = new SalesContractTag(owner, "VIP");
        UnitTestFixtures.setId(tag, 5L);

        guide.setTags(Set.of(tag));

        assertThat(guide.getTags()).containsExactly(tag);
    }

    @Test
    void setTags_null_dejaColeccionVacia() {
        var owner = UnitTestFixtures.proveedor(1L);
        var guide = guide(owner);
        var tag = new SalesContractTag(owner, "VIP");
        guide.setTags(Set.of(tag));

        guide.setTags(null);

        assertThat(guide.getTags()).isEmpty();
    }

    private static SalesGuide guide(biblioteca.gorbits.user.UserAccount owner) {
        return new SalesGuide(
                owner,
                UnitTestFixtures.campaign(1L, "C"),
                UnitTestFixtures.client(2L, owner, "Cli"),
                GuideStatus.ACTIVA,
                "000001",
                LocalDate.of(2026, 3, 1),
                Instant.parse("2026-03-01T12:00:00Z"),
                "nota inicial");
    }
}
