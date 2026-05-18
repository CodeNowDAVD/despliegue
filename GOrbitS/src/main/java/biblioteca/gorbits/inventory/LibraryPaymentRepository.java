package biblioteca.gorbits.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryPaymentRepository extends JpaRepository<LibraryPayment, Long> {

    @Query("select coalesce(sum(p.amount), 0) from LibraryPayment p where p.owner.id = :ownerId")
    BigDecimal sumAmountByOwner(@Param("ownerId") Long ownerId);

    @Query(
            """
            select coalesce(sum(p.amount), 0) from LibraryPayment p
            where p.owner.id = :ownerId and p.campaign.id = :campaignId""")
    BigDecimal sumAmountByOwnerAndCampaign(@Param("ownerId") Long ownerId, @Param("campaignId") Long campaignId);

    @EntityGraph(attributePaths = {"campaign"})
    @Query("select p from LibraryPayment p where p.owner.id = :ownerId order by p.paidOn desc, p.id desc")
    List<LibraryPayment> findForOwnerOrderByPaidOnDesc(@Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"campaign"})
    Optional<LibraryPayment> findByIdAndOwner_Id(Long id, Long ownerId);
}
