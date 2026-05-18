package biblioteca.gorbits.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserAccountUserDetailsServiceTest {

    @Mock
    UserAccountRepository users;

    @InjectMocks
    UserAccountUserDetailsService service;

    @Test
    void loadUserByUsername_devuelvePrincipal() {
        var account = UnitTestFixtures.admin(1L);
        when(users.findByUsername("admin")).thenReturn(Optional.of(account));

        var details = service.loadUserByUsername("admin");

        assertThat(details).isInstanceOf(AccountPrincipal.class);
        assertThat(((AccountPrincipal) details).account()).isSameAs(account);
    }

    @Test
    void loadUserByUsername_lanzaSiNoExiste() {
        when(users.findByUsername("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("x")).isInstanceOf(UsernameNotFoundException.class);
    }
}
