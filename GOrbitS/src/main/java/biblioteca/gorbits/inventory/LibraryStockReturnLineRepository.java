package biblioteca.gorbits.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryStockReturnLineRepository extends JpaRepository<LibraryStockReturnLine, Long> {

    @Query(
            """
            select coalesce(sum(l.quantity), 0) from LibraryStockReturnLine l
            where l.invoiceLine.id = :invoiceLineId
            """)
    int sumReturnedQuantityForInvoiceLine(@Param("invoiceLineId") Long invoiceLineId);

    @Query(
            """
            select coalesce(sum(l.quantity), 0) from LibraryStockReturnLine l
            join l.parentReturn r
            where r.owner.id = :ownerId and l.book.id = :bookId
            """)
    int sumReturnedQtyByOwnerAndBook(@Param("ownerId") long ownerId, @Param("bookId") long bookId);

    @Query(
            """
            select l.book.id, coalesce(sum(l.quantity), 0) from LibraryStockReturnLine l
            join l.parentReturn r
            where r.owner.id = :ownerId
            group by l.book.id
            """)
    List<Object[]> sumReturnedByBookForOwner(@Param("ownerId") long ownerId);
}
