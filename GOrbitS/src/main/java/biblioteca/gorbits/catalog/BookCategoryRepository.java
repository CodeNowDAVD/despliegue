package biblioteca.gorbits.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<BookCategory> findByNameIgnoreCase(String name);
}
