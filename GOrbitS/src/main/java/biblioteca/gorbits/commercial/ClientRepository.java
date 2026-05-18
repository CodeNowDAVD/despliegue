package biblioteca.gorbits.commercial;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByOwner_IdOrderByFullNameAsc(Long ownerId);

    List<Client> findByOwner_IdAndFullNameContainingIgnoreCaseOrderByFullNameAsc(Long ownerId, String fullName);

    Optional<Client> findByIdAndOwner_Id(Long id, Long ownerId);
}
