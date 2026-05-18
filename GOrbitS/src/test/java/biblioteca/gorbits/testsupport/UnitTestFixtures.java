package biblioteca.gorbits.testsupport;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookCategory;
import biblioteca.gorbits.catalog.BookType;
import biblioteca.gorbits.commercial.Campaign;
import biblioteca.gorbits.commercial.Client;
import biblioteca.gorbits.commercial.GuideStatus;
import biblioteca.gorbits.commercial.ProviderProfile;
import biblioteca.gorbits.commercial.SalesGuide;
import biblioteca.gorbits.commercial.SalesZone;
import biblioteca.gorbits.user.Role;
import biblioteca.gorbits.user.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.test.util.ReflectionTestUtils;

/** Datos mínimos reutilizables en pruebas unitarias (sin Spring). */
public final class UnitTestFixtures {

    private UnitTestFixtures() {}

    public static UserAccount user(long id, String username, Role role) {
        UserAccount u = new UserAccount(username, "hash", role, true);
        setId(u, id);
        return u;
    }

    public static UserAccount proveedor(long id) {
        return user(id, "proveedor", Role.PROVEEDOR);
    }

    public static UserAccount admin(long id) {
        return user(id, "admin", Role.ADMIN);
    }

    public static BookCategory category(long id, String name) {
        BookCategory c = new BookCategory(name);
        setId(c, id);
        return c;
    }

    public static Book book(long id, BookCategory category, String title, BigDecimal price) {
        Book b = new Book(category, title, price, BookType.UNITARIO, null);
        setId(b, id);
        return b;
    }

    public static SalesZone zone(long id, String name) {
        SalesZone z = new SalesZone(name);
        setId(z, id);
        return z;
    }

    public static Campaign campaign(long id, String name) {
        Campaign c = new Campaign(name, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));
        setId(c, id);
        return c;
    }

    public static Client client(long id, UserAccount owner, String fullName) {
        Client c = new Client(owner, fullName, null, null, null);
        setId(c, id);
        return c;
    }

    public static ProviderProfile providerProfile(long id, UserAccount user, SalesZone zone) {
        ProviderProfile p = new ProviderProfile(user, zone);
        setId(p, id);
        return p;
    }

    public static SalesGuide guide(
            long id,
            UserAccount owner,
            Campaign campaign,
            Client client,
            GuideStatus status) {
        SalesGuide g = new SalesGuide(
                owner,
                campaign,
                client,
                status,
                "000001",
                LocalDate.of(2026, 3, 1),
                Instant.parse("2026-03-01T12:00:00Z"),
                null);
        setId(g, id);
        return g;
    }

    public static void setId(Object entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
