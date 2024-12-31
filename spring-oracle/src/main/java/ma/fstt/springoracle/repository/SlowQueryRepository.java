package ma.fstt.springoracle.repository;

import ma.fstt.springoracle.model.SlowQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlowQueryRepository extends JpaRepository<SlowQuery, Long> {
    List<SlowQuery> findByExecutionTimeGreaterThan(Long executionTime);
}

