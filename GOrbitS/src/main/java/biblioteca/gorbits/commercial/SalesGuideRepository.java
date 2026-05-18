package biblioteca.gorbits.commercial;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesGuideRepository extends JpaRepository<SalesGuide, Long> {

    boolean existsByOwner_IdAndContractNumber(Long ownerId, String contractNumber);

    long countByOwner_IdAndCampaign_IdAndStatus(Long ownerId, Long campaignId, GuideStatus status);

    long countByStatus(GuideStatus status);

    /**
     * Suma de unidades por libro en guías con el estado dado (p. ej. CERRADA para ranking de ventas efectivas).
     */
    @Query(
            """
            select l.book.id, l.book.title, sum(l.quantity)
            from SalesGuideLine l
            join l.guide g
            where g.status = :status
            group by l.book.id, l.book.title
            order by sum(l.quantity) desc
            """)
    List<Object[]> sumUnitsByBookForGuideStatus(@Param("status") GuideStatus status, Pageable pageable);

    /**
     * Listado sin líneas; trae campaña y cliente para mostrar en tabla.
     */
    @EntityGraph(attributePaths = {"campaign", "client", "tags"})
    List<SalesGuide> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = {"campaign", "client", "tags"})
    List<SalesGuide> findByOwner_IdAndContractNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            Long ownerId, String contractNumber);

    @EntityGraph(attributePaths = {"campaign", "client", "tags"})
    @Query(
            """
            select distinct g from SalesGuide g
            join g.tags t
            where g.owner.id = :ownerId and t.id = :tagId
            order by g.createdAt desc
            """)
    List<SalesGuide> findByOwner_IdAndTag_IdOrderByCreatedAtDesc(
            @Param("ownerId") Long ownerId, @Param("tagId") Long tagId);

    @EntityGraph(attributePaths = {"campaign", "client", "tags"})
    @Query(
            """
            select distinct g from SalesGuide g
            join g.tags t
            where g.owner.id = :ownerId and t.id = :tagId
            and lower(g.contractNumber) like lower(concat('%', :q, '%'))
            order by g.createdAt desc
            """)
    List<SalesGuide> findByOwner_IdAndTag_IdAndContractNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            @Param("ownerId") Long ownerId, @Param("tagId") Long tagId, @Param("q") String q);

    @EntityGraph(attributePaths = {"campaign", "client", "lines", "lines.book", "tags"})
    Optional<SalesGuide> findDetailedByIdAndOwner_Id(Long id, Long ownerId);

    Optional<SalesGuide> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByClient_Id(Long clientId);

    long countByOwner_IdAndClient_Id(Long ownerId, Long clientId);

    long countByOwner_IdAndClient_IdAndCampaign_Id(Long ownerId, Long clientId, Long campaignId);

    @EntityGraph(attributePaths = {"campaign", "client"})
    @Query(
            "select g from SalesGuide g where g.owner.id = :ownerId and g.status = :st order by g.clientReturnAt desc nulls last, g.id desc")
    List<SalesGuide> findByOwner_IdAndStatusForReturns(
            @Param("ownerId") Long ownerId, @Param("st") GuideStatus status);

    @Query(
            "select coalesce(sum(l.quantity), 0) from SalesGuideLine l join l.guide g where g.owner.id = :ownerId and g.status = :st")
    long sumLineQtyForOwnerAndStatus(@Param("ownerId") long ownerId, @Param("st") GuideStatus status);

    @Query(
            """
            select coalesce(sum(l.quantity), 0) from SalesGuideLine l join l.guide g
            where g.owner.id = :ownerId and g.campaign.id = :campaignId and g.status = :st""")
    long sumLineQtyForOwnerCampaignAndStatus(
            @Param("ownerId") long ownerId, @Param("campaignId") long campaignId, @Param("st") GuideStatus status);

    @EntityGraph(attributePaths = {"lines", "lines.book"})
    List<SalesGuide> findByOwner_IdAndCampaign_IdAndStatus(
            Long ownerId, Long campaignId, GuideStatus status);
}
