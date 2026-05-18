package biblioteca.gorbits.inventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryStockReturnRepository extends JpaRepository<LibraryStockReturn, Long> {

    @EntityGraph(attributePaths = {"campaign", "lines"})
    @Query("select r from LibraryStockReturn r where r.owner.id = :ownerId order by r.createdAt desc")
    List<LibraryStockReturn> findByOwner_IdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);

    @Query(
            "select coalesce(sum(line.quantity), 0) from LibraryStockReturn r join r.lines line where r.owner.id = :ownerId")
    long sumLineQtyByOwner(@Param("ownerId") Long ownerId);

    @Query(
            """
            select coalesce(sum(line.quantity), 0) from LibraryStockReturn r join r.lines line
            where r.owner.id = :ownerId and r.campaign.id = :campaignId""")
    long sumLineQtyByOwnerAndCampaign(@Param("ownerId") Long ownerId, @Param("campaignId") Long campaignId);

    @EntityGraph(attributePaths = {"lines", "lines.book", "lines.invoiceLine", "campaign"})
    @Query(
            "select r from LibraryStockReturn r where r.id = :id and r.owner.id = :ownerId")
    Optional<LibraryStockReturn> findDetailedByIdAndOwner_Id(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
