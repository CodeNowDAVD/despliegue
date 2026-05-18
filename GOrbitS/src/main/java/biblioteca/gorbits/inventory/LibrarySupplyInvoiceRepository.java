package biblioteca.gorbits.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibrarySupplyInvoiceRepository extends JpaRepository<LibrarySupplyInvoice, Long> {

    @Query("select coalesce(sum(line.quantity), 0) from LibrarySupplyInvoice inv join inv.lines line")
    long sumAllPurchasedUnits();

    @Query(
            """
            select coalesce(sum(coalesce(line.lineTotal, 0)), 0)
            from LibrarySupplyInvoice inv join inv.lines line where inv.owner.id = :ownerId""")
    BigDecimal sumAllLineAmountsByOwner(@Param("ownerId") long ownerId);

    @Query(
            "select coalesce(sum(line.quantity), 0) from LibrarySupplyInvoice inv join inv.lines line where inv.owner.id = :ownerId")
    long sumPurchasedUnitsByOwner(@Param("ownerId") long ownerId);

    @Query(
            """
            select coalesce(sum(coalesce(line.lineTotal, 0)), 0)
            from LibrarySupplyInvoice inv join inv.lines line
            where inv.owner.id = :ownerId and inv.issuedOn between :from and :to""")
    BigDecimal sumLineAmountIssuedBetweenForOwner(
            @Param("ownerId") long ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
            select coalesce(sum(line.quantity), 0)
            from LibrarySupplyInvoice inv join inv.lines line
            where inv.owner.id = :ownerId and inv.issuedOn between :from and :to""")
    long sumLineQtyIssuedBetweenForOwner(
            @Param("ownerId") long ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    boolean existsByOwner_IdAndInvoiceNumberIgnoreCase(long ownerId, String invoiceNumber);

    @Query(
            """
            select distinct i from LibrarySupplyInvoice i
            join fetch i.owner
            left join fetch i.lines l
            left join fetch l.book
            order by i.createdAt desc""")
    List<LibrarySupplyInvoice> findAllWithLines();

    @Query(
            """
            select distinct i from LibrarySupplyInvoice i
            join fetch i.owner
            left join fetch i.lines l
            left join fetch l.book
            where i.owner.id = :ownerId
            order by i.createdAt desc""")
    List<LibrarySupplyInvoice> findAllWithLinesByOwner_Id(@Param("ownerId") long ownerId);

    @Query(
            """
            select distinct i from LibrarySupplyInvoice i
            join fetch i.owner
            left join fetch i.lines l
            left join fetch l.book
            where i.id = :id""")
    Optional<LibrarySupplyInvoice> findDetailedById(@Param("id") Long id);

    @Query(
            """
            select distinct i from LibrarySupplyInvoice i
            join fetch i.owner
            left join fetch i.lines l
            left join fetch l.book
            where i.id = :id and i.owner.id = :ownerId""")
    Optional<LibrarySupplyInvoice> findDetailedByIdAndOwner_Id(@Param("id") Long id, @Param("ownerId") long ownerId);
}
