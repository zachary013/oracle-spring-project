package ma.fstt.springoracle.service;

import ma.fstt.springoracle.model.PerformanceMetrics;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PerformanceMonitoringService {

    public List<Map<String, Object>> getAWRReport(LocalDateTime startTime, LocalDateTime endTime) ;

    public List<Map<String, Object>> getASHReport() ;

    public PerformanceMetrics getRealTimeMetrics() ;
}
