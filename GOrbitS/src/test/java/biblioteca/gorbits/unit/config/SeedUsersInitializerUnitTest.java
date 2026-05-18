package biblioteca.gorbits.unit.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import biblioteca.gorbits.config.SeedUsersInitializer;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SeedUsersInitializerUnitTest {

    @Mock
    UserAccountRepository users;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    SeedUsersInitializer initializer;

    @Test
    void run_usuariosExistentes_noGuarda() throws Exception {
        when(users.findByUsername("admin")).thenReturn(Optional.of(UnitTestFixtures.admin(1L)));
        when(users.findByUsername("proveedor")).thenReturn(Optional.of(UnitTestFixtures.proveedor(2L)));

        initializer.run();

        verify(users, never()).save(any());
    }

    @Test
    void run_faltanUsuarios_crea() throws Exception {
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        when(users.findByUsername("proveedor")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        initializer.run();

        verify(users, times(2)).save(any());
    }
}
