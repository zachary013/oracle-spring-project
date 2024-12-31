package ma.fstt.springoracle.repository;

import ma.fstt.springoracle.model.PerformanceMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PerformanceMetricsRepository extends JpaRepository<PerformanceMetrics, Long> {
    List<PerformanceMetrics> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}