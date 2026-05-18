package biblioteca.gorbits.config;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookCategory;
import biblioteca.gorbits.catalog.BookCategoryRepository;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.catalog.BookType;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"h2", "test"})
@Order(200)
public class SeedCatalogInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedCatalogInitializer.class);

    /** Libro marcador: si existe, el catálogo demo ya está cargado. */
    private static final String MARKER_BOOK_TITLE = "El maravilloso mundo de la Biblia";

    private final BookCategoryRepository categories;
    private final BookRepository books;

    public SeedCatalogInitializer(BookCategoryRepository categories, BookRepository books) {
        this.categories = categories;
        this.books = books;
    }

    @Override
    public void run(String... args) {
        if (books.findAllByOrderByTitleAsc().stream()
                .anyMatch(b -> MARKER_BOOK_TITLE.equals(b.getTitle()))) {
            return;
        }
        BookCategory salud = ensureCategory("Salud");
        BookCategory familia = ensureCategory("Familia");

        books.save(new Book(salud, MARKER_BOOK_TITLE, new BigDecimal("18.00"), BookType.UNITARIO, null));
        books.save(new Book(salud, "Saludablemente", new BigDecimal("20.00"), BookType.UNITARIO, null));

        Book vivirConEsperanza = books.save(
                new Book(salud, "Vivir con Esperanza", new BigDecimal("2.50"), BookType.UNITARIO, null));

        Book bebidas = books.save(new Book(
                salud,
                "Bebidas saludables nutritivas y deliciosas",
                new BigDecimal("24.00"),
                BookType.PAQUETE,
                "Manual librería **: suele facturarse también «Vivir con Esperanza»."));
        bebidas.setCompanionBook(vivirConEsperanza);
        bebidas.setCompanionLinePrice(vivirConEsperanza.getPrice());
        books.save(bebidas);

        books.save(new Book(familia, "Educación en el hogar", new BigDecimal("15.00"), BookType.UNITARIO, null));

        log.info("Catálogo de ejemplo cargado (manual librería: * y **).");
    }

    private BookCategory ensureCategory(String name) {
        return categories.findByNameIgnoreCase(name).orElseGet(() -> categories.save(new BookCategory(name)));
    }
}
