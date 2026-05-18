package biblioteca.gorbits.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByCategoryIdOrderByTitleAsc(Long categoryId);

    List<Book> findAllByOrderByTitleAsc();

    List<Book> findByCompanionBook_Id(Long companionBookId);

    boolean existsByCategoryId(Long categoryId);
}
