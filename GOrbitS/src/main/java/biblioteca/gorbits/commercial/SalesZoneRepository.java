package biblioteca.gorbits.commercial;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesZoneRepository extends JpaRepository<SalesZone, Long> {

    List<SalesZone> findAllByOrderByNameAsc();

    Optional<SalesZone> findFirstByOrderByIdAsc();
}
