package biblioteca.gorbits.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {

    @Query(
            """
            select b.category.id, b.category.name, coalesce(sum(w.quantity), 0)
            from WarehouseStock w
            join w.book b
            where w.quantity > 0
            group by b.category.id, b.category.name
            order by b.category.name asc
            """)
    List<Object[]> sumPositiveUnitsByCategory();

    @Query("select coalesce(sum(w.quantity), 0) from WarehouseStock w")
    long sumTotalQuantity();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WarehouseStock w join fetch w.book b where b.id = :bookId")
    Optional<WarehouseStock> findForUpdateByBook_Id(@Param("bookId") Long bookId);

    Optional<WarehouseStock> findByBook_Id(Long bookId);
}
