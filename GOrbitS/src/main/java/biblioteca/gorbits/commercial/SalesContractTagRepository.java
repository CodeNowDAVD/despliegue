package biblioteca.gorbits.commercial;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesContractTagRepository extends JpaRepository<SalesContractTag, Long> {

    List<SalesContractTag> findByOwner_IdOrderByNameAsc(Long ownerId);

    Optional<SalesContractTag> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByOwner_IdAndNameIgnoreCase(Long ownerId, String name);
}
