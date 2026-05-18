package biblioteca.gorbits.unit.config;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.config.ClientPaymentMessageProperties;
import biblioteca.gorbits.config.JwtProperties;
import org.junit.jupiter.api.Test;

class ConfigPropertiesUnitTest {

    @Test
    void jwtProperties_record() {
        var p = new JwtProperties("secret-de-al-menos-32-bytes-utf8!!", 3600_000L);
        assertThat(p.secret()).contains("secret");
        assertThat(p.expirationMs()).isEqualTo(3600_000L);
    }

    @Test
    void clientPaymentMessage_defaults() {
        var p = new ClientPaymentMessageProperties(null, null);
        assertThat(p.currencySymbol()).isEqualTo("S/");
        assertThat(p.template()).contains("{cliente}");
    }

    @Test
    void clientPaymentMessage_custom() {
        var p = new ClientPaymentMessageProperties("USD", "Hola {cliente}");
        assertThat(p.currencySymbol()).isEqualTo("USD");
        assertThat(p.template()).isEqualTo("Hola {cliente}");
    }

    @Test
    void clientPaymentMessage_blancos_usanDefault() {
        var p = new ClientPaymentMessageProperties("   ", "  ");
        assertThat(p.currencySymbol()).isEqualTo("S/");
        assertThat(p.template()).contains("{guiaId}");
    }
}
