package biblioteca.gorbits.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderFieldStockRepository extends JpaRepository<ProviderFieldStock, Long> {

    @Query(
            """
            select b.category.id, b.category.name, coalesce(sum(p.quantity), 0)
            from ProviderFieldStock p
            join p.book b
            where p.owner.id = :ownerId and p.quantity > 0
            group by b.category.id, b.category.name
            order by b.category.name asc
            """)
    List<Object[]> sumPositiveUnitsByCategoryForOwner(@Param("ownerId") Long ownerId);

    @Query("select coalesce(sum(p.quantity), 0) from ProviderFieldStock p")
    long sumTotalQuantity();

    Optional<ProviderFieldStock> findByOwner_IdAndBook_Id(Long ownerId, Long bookId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select p from ProviderFieldStock p join fetch p.book b where p.owner.id = :ownerId and b.id = :bookId")
    Optional<ProviderFieldStock> findForUpdateByOwner_IdAndBook_Id(
            @Param("ownerId") Long ownerId, @Param("bookId") Long bookId);

    @Query(
            "select p from ProviderFieldStock p join fetch p.book b where p.owner.id = :ownerId and p.quantity > 0 order by b.title asc")
    List<ProviderFieldStock> findPositiveByOwner_Id(@Param("ownerId") Long ownerId);
}
