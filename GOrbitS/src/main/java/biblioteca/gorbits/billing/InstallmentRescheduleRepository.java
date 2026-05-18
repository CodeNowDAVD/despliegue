package biblioteca.gorbits.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRescheduleRepository extends JpaRepository<InstallmentReschedule, Long> {

    List<InstallmentReschedule> findByInstallment_IdOrderByRescheduledAtDesc(Long installmentId);
}
