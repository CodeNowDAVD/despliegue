package biblioteca.gorbits.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import biblioteca.gorbits.config.JwtProperties;
import biblioteca.gorbits.testsupport.UnitTestFixtures;
import biblioteca.gorbits.user.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties("test-jwt-secret-key-at-least-32-chars-long!", 3600_000L));
        jwtService.initKey();
    }

    @Test
    void createTokenYParseAuthentication_roundTrip() {
        var account = UnitTestFixtures.admin(1L);
        var principal = new AccountPrincipal(account);

        String token = jwtService.createToken(principal);
        Authentication auth = jwtService.parseAuthentication(token);

        assertThat(auth.getName()).isEqualTo("admin");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void parseAuthentication_rechazaTokenInvalido() {
        assertThatThrownBy(() -> jwtService.parseAuthentication("token.invalido"))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token inválido");
    }

    @Test
    void initKey_rechazaSecretoCorto() {
        JwtService svc = new JwtService(new JwtProperties("corto", 1000L));
        assertThatThrownBy(svc::initKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void createToken_incluyeRolProveedor() {
        var principal = new AccountPrincipal(UnitTestFixtures.user(2L, "emb01", Role.PROVEEDOR));
        Authentication auth = jwtService.parseAuthentication(jwtService.createToken(principal));
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_PROVEEDOR");
    }

    @Test
    void parseAuthentication_rechazaTokenSinRol() {
        var key = Keys.hmacShaKeyFor(
                "test-jwt-secret-key-at-least-32-chars-long!".getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        String token = Jwts.builder()
                .subject("admin")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAuthentication(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token inválido");
    }
}
