package biblioteca.gorbits.config;

import biblioteca.gorbits.catalog.Book;
import biblioteca.gorbits.catalog.BookRepository;
import biblioteca.gorbits.inventory.WarehouseStock;
import biblioteca.gorbits.inventory.WarehouseStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile({"h2", "test"})
@Order(400)
public class SeedInventoryInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedInventoryInitializer.class);

    private static final int INITIAL_WAREHOUSE_UNITS_PER_BOOK = 500;

    private final BookRepository books;
    private final WarehouseStockRepository warehouse;

    public SeedInventoryInitializer(BookRepository books, WarehouseStockRepository warehouse) {
        this.books = books;
        this.warehouse = warehouse;
    }

    @Override
    public void run(String... args) {
        if (books.count() == 0) {
            return;
        }
        int added = 0;
        for (Book b : books.findAll()) {
            if (warehouse.findByBook_Id(b.getId()).isEmpty()) {
                warehouse.save(new WarehouseStock(b, INITIAL_WAREHOUSE_UNITS_PER_BOOK));
                added++;
            }
        }
        if (added > 0) {
            log.info(
                    "Stock inicial de almacén cargado ({} unidades por libro, {} títulos nuevos).",
                    INITIAL_WAREHOUSE_UNITS_PER_BOOK,
                    added);
        }
    }
}
