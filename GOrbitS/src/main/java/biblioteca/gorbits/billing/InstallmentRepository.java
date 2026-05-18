package biblioteca.gorbits.billing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    boolean existsByGuide_Id(Long guideId);

    @EntityGraph(attributePaths = {"payments"})
    List<Installment> findByGuide_IdOrderBySeqAsc(Long guideId);

    @EntityGraph(attributePaths = {"guide", "guide.client"})
    @Query(
            """
            select i from Installment i
            join i.guide g
            where g.owner.id = :ownerId
            and g.status <> biblioteca.gorbits.commercial.GuideStatus.DEVUELTA
            and i.dueDate between :from and :to
            order by i.dueDate asc, i.seq asc
            """)
    List<Installment> findDueBetween(
            @Param("ownerId") Long ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
            select i from Installment i
            join i.guide g
            where i.id = :id and g.owner.id = :ownerId
            """)
    Optional<Installment> findByIdAndOwner_Id(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"guide", "guide.client", "payments"})
    @Query(
            """
            select i from Installment i
            where i.id = :id and i.guide.owner.id = :ownerId
            """)
    Optional<Installment> findDetailByIdAndOwner_Id(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"payments", "guide", "guide.client", "guide.campaign"})
    @Query(
            """
            select i from Installment i
            join i.guide g
            where g.owner.id = :ownerId
            and (:campaignId is null or g.campaign.id = :campaignId)
            order by i.dueDate asc, i.seq asc
            """)
    List<Installment> findForOwnerBilling(
            @Param("ownerId") Long ownerId, @Param("campaignId") Long campaignId);

    @EntityGraph(attributePaths = {"payments", "guide", "guide.client", "guide.campaign"})
    @Query(
            """
            select i from Installment i
            join i.guide g
            where g.owner.id = :ownerId and g.client.id = :clientId
            and (:campaignId is null or g.campaign.id = :campaignId)
            order by i.dueDate asc, i.seq asc
            """)
    List<Installment> findForClientBilling(
            @Param("ownerId") Long ownerId,
            @Param("clientId") Long clientId,
            @Param("campaignId") Long campaignId);
}
