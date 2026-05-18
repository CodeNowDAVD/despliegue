package biblioteca.gorbits.unit.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.Client;
import biblioteca.gorbits.commercial.GuideLifecycleRules;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prueba unitaria de reglas puras (sin Spring ni repositorios).
 * Paquete {@code unit} — alineado con guía de Testing del Software.
 */
@ExtendWith(MockitoExtension.class)
class GuideLifecycleRulesUnitTest {

    private UserAccount owner;
    private Campaign campaign;
    private Client client;

    @BeforeEach
    void setUp() {
        owner = UnitTestFixtures.proveedor(1L);
        campaign = UnitTestFixtures.campaign(1L, "Campaña demo");
        client = UnitTestFixtures.client(1L, owner, "Cliente");
    }

    @ParameterizedTest(name = "status={0}, clientReturnAt={1} → devuelta={2}")
    @MethodSource("casosIsReturned")
    void isReturned_parametrizado(GuideStatus status, Instant clientReturnAt, boolean esperado) {
        SalesGuide guide = UnitTestFixtures.guide(1L, owner, campaign, client, status);
        if (clientReturnAt != null) {
            guide.setClientReturnMeta(clientReturnAt, "motivo", false);
        }
        assertThat(GuideLifecycleRules.isReturned(guide)).isEqualTo(esperado);
    }

    static Stream<Arguments> casosIsReturned() {
        return Stream.of(
                Arguments.of(GuideStatus.ACTIVA, null, false),
                Arguments.of(GuideStatus.CERRADA, null, false),
                Arguments.of(GuideStatus.DEVUELTA, null, true),
                Arguments.of(GuideStatus.ACTIVA, Instant.parse("2026-01-15T10:00:00Z"), true));
    }

    @Test
    void assertMutable_guiaActiva_noLanza() {
        SalesGuide guide = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.ACTIVA);
        GuideLifecycleRules.assertMutable(guide);
    }

    @Test
    void assertMutable_guiaDevuelta_lanzaIllegalState() {
        SalesGuide guide = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.DEVUELTA);
        assertThatThrownBy(() -> GuideLifecycleRules.assertMutable(guide))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("devuelto");
    }
}
