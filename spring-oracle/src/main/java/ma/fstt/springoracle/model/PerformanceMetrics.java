package ma.fstt.springoracle.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    private Double cpuUsagePercent;

    private Double memoryUsageMB;

    private Double bufferCacheHitRatio;

    private Double ioOperationsPerSecond;

    private Double pgaUsageMB;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
}
