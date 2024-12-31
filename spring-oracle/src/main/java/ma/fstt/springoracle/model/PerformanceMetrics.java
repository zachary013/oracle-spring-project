package ma.fstt.springoracle.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "performance_metrics")
@Data
public class PerformanceMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double cpuUsage;

    private Double memoryUsage;

    private Double ioUsage;

    private LocalDateTime timestamp;
}
