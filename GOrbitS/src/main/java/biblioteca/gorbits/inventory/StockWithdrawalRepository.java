package biblioteca.gorbits.inventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockWithdrawalRepository extends JpaRepository<StockWithdrawal, Long> {

    @Query(
            "select distinct w from StockWithdrawal w left join fetch w.lines l left join fetch l.book b where w.owner.id = :ownerId order by w.createdAt desc")
    List<StockWithdrawal> findByOwner_IdWithLines(@Param("ownerId") Long ownerId);

    @Query(
            "select distinct w from StockWithdrawal w left join fetch w.lines l left join fetch l.book b where w.id = :id and w.owner.id = :ownerId")
    Optional<StockWithdrawal> findDetailedByIdAndOwner_Id(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
