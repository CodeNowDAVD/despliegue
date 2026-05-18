package biblioteca.gorbits.security;

import static org.assertj.core.api.Assertions.assertThat;

import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.Role;
import org.junit.jupiter.api.Test;

class AccountPrincipalTest {

    @Test
    void exponeDatosDeCuentaYAutoridad() {
        var account = UnitTestFixtures.user(1L, "admin", Role.ADMIN);
        var principal = new AccountPrincipal(account);

        assertThat(principal.account()).isSameAs(account);
        assertThat(principal.getUsername()).isEqualTo("admin");
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }
}
