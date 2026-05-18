package biblioteca.gorbits.security;

import biblioteca.gorbits.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initKey() {
        byte[] bytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.security.jwt.secret debe tener al menos 32 bytes (UTF-8)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(AccountPrincipal principal) {
        var acc = principal.account();
        Date now = new Date();
        Date exp = new Date(now.getTime() + properties.expirationMs());
        return Jwts.builder()
                .subject(acc.getUsername())
                .claim(CLAIM_ROLE, acc.getRole().name())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Authentication parseAuthentication(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String username = claims.getSubject();
            String role = claims.get(CLAIM_ROLE, String.class);
            if (role == null) {
                throw new JwtException("Token sin rol");
            }
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (JwtException e) {
            throw new JwtException("Token inválido", e);
        }
    }
}
