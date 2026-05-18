package biblioteca.gorbits.inventory;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibrarySupplyInvoiceLineRepository extends JpaRepository<LibrarySupplyInvoiceLine, Long> {

    @Query(
            """
            select l from LibrarySupplyInvoiceLine l
            join fetch l.invoice inv
            join fetch l.book
            where l.id = :lineId and inv.owner.id = :ownerId
            """)
    Optional<LibrarySupplyInvoiceLine> findByIdAndInvoiceOwner_Id(
            @Param("lineId") Long lineId, @Param("ownerId") Long ownerId);

    @Query(
            """
            select coalesce(sum(l.quantity), 0) from LibrarySupplyInvoiceLine l
            join l.invoice i where i.owner.id = :ownerId and l.book.id = :bookId
            """)
    int sumPurchasedQtyByOwnerAndBook(@Param("ownerId") long ownerId, @Param("bookId") long bookId);

    @Query(
            """
            select l.book.id, l.book.title, l.book.category.id, l.book.category.name, coalesce(sum(l.quantity), 0)
            from LibrarySupplyInvoiceLine l join l.invoice i
            where i.owner.id = :ownerId
            group by l.book.id, l.book.title, l.book.category.id, l.book.category.name
            """)
    List<Object[]> sumPurchasedByBookForOwner(@Param("ownerId") long ownerId);

    @Query(
            """
            select l from LibrarySupplyInvoiceLine l
            join fetch l.invoice i
            join fetch l.book
            where i.owner.id = :ownerId and l.book.id = :bookId
            order by i.issuedOn asc, l.id asc
            """)
    List<LibrarySupplyInvoiceLine> findReturnableLinesForOwnerAndBook(
            @Param("ownerId") long ownerId, @Param("bookId") long bookId);
}
