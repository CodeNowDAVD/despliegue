package biblioteca.gorbits.commercial;

import biblioteca.gorbits.user.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderProfileRepository extends JpaRepository<ProviderProfile, Long> {

    @EntityGraph(attributePaths = {"zone", "user"})
    Optional<ProviderProfile> findWithZoneByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    boolean existsByDni(String dni);

    boolean existsByDniAndUser_IdNot(String dni, Long userId);

    @EntityGraph(attributePaths = {"zone", "user"})
    @Query("SELECT p FROM ProviderProfile p JOIN p.user u WHERE u.role = :role ORDER BY p.lastName, p.firstName, u.username")
    List<ProviderProfile> findAllByUserRole(@Param("role") Role role);
}
