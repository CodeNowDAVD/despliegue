package biblioteca.gorbits.commercial;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesGuideLineRepository extends JpaRepository<SalesGuideLine, Long> {

    @Query(
            """
            select coalesce(sum(l.quantity), 0) from SalesGuideLine l
            join l.guide g
            where g.owner.id = :ownerId and l.book.id = :bookId and g.status <> biblioteca.gorbits.commercial.GuideStatus.DEVUELTA
            """)
    int sumSoldQtyByOwnerAndBook(@Param("ownerId") long ownerId, @Param("bookId") long bookId);

    @Query(
            """
            select l.book.id, coalesce(sum(l.quantity), 0) from SalesGuideLine l
            join l.guide g
            where g.owner.id = :ownerId and g.status <> biblioteca.gorbits.commercial.GuideStatus.DEVUELTA
            group by l.book.id
            """)
    List<Object[]> sumSoldByBookForOwner(@Param("ownerId") long ownerId);
}
