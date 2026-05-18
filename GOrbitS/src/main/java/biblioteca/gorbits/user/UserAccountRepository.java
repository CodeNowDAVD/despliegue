package biblioteca.gorbits.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByIdAndRole(long id, Role role);

    List<UserAccount> findAllByOrderByUsernameAsc();

    List<UserAccount> findByRoleOrderByUsernameAsc(Role role);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, long id);

    long countByRole(Role role);
}
