package biblioteca.gorbits.commercial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GuideLifecycleRulesTest {

    private final UserAccount owner = UnitTestFixtures.proveedor(1L);
    private final Campaign campaign = UnitTestFixtures.campaign(1L, "Campaña 2026");
    private final Client client = UnitTestFixtures.client(1L, owner, "Cliente Demo");

    @Test
    void isReturned_cuandoEstadoDevuelta() {
        SalesGuide g = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.DEVUELTA);
        assertThat(GuideLifecycleRules.isReturned(g)).isTrue();
    }

    @Test
    void isReturned_cuandoHayFechaDevolucionCliente() {
        SalesGuide g = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.ACTIVA);
        g.setClientReturnMeta(Instant.now(), "motivo", false);
        assertThat(GuideLifecycleRules.isReturned(g)).isTrue();
    }

    @Test
    void assertMutable_rechazaGuiaDevuelta() {
        SalesGuide g = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.DEVUELTA);
        assertThatThrownBy(() -> GuideLifecycleRules.assertMutable(g))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("devuelto");
    }

    @Test
    void assertStatusPatchAllowed_rechazaMarcarDevueltaDirectamente() {
        SalesGuide g = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.ACTIVA);
        assertThatThrownBy(() -> GuideLifecycleRules.assertStatusPatchAllowed(g, GuideStatus.DEVUELTA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Registrar devolución");
    }

    @Test
    void assertClientReturnAllowed_rechazaSegundaDevolucion() {
        SalesGuide g = UnitTestFixtures.guide(1L, owner, campaign, client, GuideStatus.DEVUELTA);
        assertThatThrownBy(() -> GuideLifecycleRules.assertClientReturnAllowed(g))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue registrada");
    }
}
