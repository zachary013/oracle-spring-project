package ma.fstt.springoracle.service;


import lombok.extern.slf4j.Slf4j;
import ma.fstt.springoracle.model.PerformanceMetrics;
import ma.fstt.springoracle.repository.PerformanceMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PerformanceMonitoringServiceImpl implements PerformanceMonitoringService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PerformanceMetricsRepository metricsRepository;

    // Get AWR Report
//    public Map<String, Object> getAWRReport(LocalDateTime startTime, LocalDateTime endTime) {
//        String sql = """
//            SELECT snap_id, begin_interval_time, end_interval_time,
//                   cpu_usage_percent, memory_usage_mb, io_requests_per_sec
//            FROM dba_hist_snapshot s
//            JOIN dba_hist_sysstat m ON s.snap_id = m.snap_id
//            WHERE begin_interval_time BETWEEN ? AND ?
//        """;
//        return jdbcTemplate.queryForMap(sql, startTime, endTime);
//    }
    public List<Map<String, Object>> getAWRReport(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
        SELECT 
            s.snap_id,
            s.begin_interval_time,
            s.end_interval_time,
            ROUND(
                (SELECT value FROM v$sysstat 
                 WHERE name = 'CPU used by this session') / 100, 2
            ) as cpu_usage_percent,
            ROUND(
                (SELECT SUM(bytes)/(1024*1024) 
                 FROM v$sgastat), 2
            ) as memory_usage_mb,
            ROUND(
                (SELECT value FROM v$sysstat 
                 WHERE name = 'physical reads'), 2
            ) as io_requests_per_sec
        FROM dba_hist_snapshot s
        WHERE s.begin_interval_time BETWEEN ? AND ?
        ORDER BY s.snap_id
    """;

        try {
            return jdbcTemplate.queryForList(sql, startTime, endTime);
        } catch (Exception e) {
//            log.error("Error retrieving AWR report: ", e);
            System.out.println(e.getMessage());
            throw new RuntimeException("Failed to retrieve AWR report: " + e.getMessage(), e);
        }
    }

    // Get ASH Report
    public List<Map<String, Object>> getASHReport() {
        String sql = """
            SELECT session_id, sql_id, event, wait_class,
                   session_state, time_waited
            FROM v$active_session_history
            WHERE sample_time > SYSDATE - 1/24
        """;
        return jdbcTemplate.queryForList(sql);
    }

    // Get Real-time Metrics
    public PerformanceMetrics getRealTimeMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        // CPU Usage
        String cpuSql = "SELECT value FROM v$sysstat WHERE name = 'CPU used by this session'";
        Double cpuUsage = jdbcTemplate.queryForObject(cpuSql, Double.class);
        metrics.setCpuUsage(cpuUsage);

        // Memory Usage
        String memorySql = "SELECT SUM(bytes)/(1024*1024) FROM v$sgastat";
        Double memoryUsage = jdbcTemplate.queryForObject(memorySql, Double.class);
        metrics.setMemoryUsage(memoryUsage);

        // IO Usage
        String ioSql = "SELECT value FROM v$sysstat WHERE name = 'physical reads'";
        Double ioUsage = jdbcTemplate.queryForObject(ioSql, Double.class);
        metrics.setIoUsage(ioUsage);

        metrics.setTimestamp(LocalDateTime.now());
        return metricsRepository.save(metrics);
    }
}

