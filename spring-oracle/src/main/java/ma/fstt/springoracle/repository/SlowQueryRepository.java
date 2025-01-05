package ma.fstt.springoracle.repository;

import ma.fstt.springoracle.model.SlowQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlowQueryRepository extends JpaRepository<SlowQuery, Long> {
    // Custom query to find slow queries by status, ordered by elapsed time descending
    List<SlowQuery> findByStatusOrderByElapsedTimeDesc(String status);
}
