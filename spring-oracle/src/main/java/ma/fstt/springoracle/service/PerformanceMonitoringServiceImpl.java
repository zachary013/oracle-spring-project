package ma.fstt.springoracle.service;


import com.sun.management.OperatingSystemMXBean;
import ma.fstt.springoracle.model.PerformanceMetrics;
import ma.fstt.springoracle.repository.PerformanceMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

//import java.lang.management.ManagementFactory;
import java.lang.management.ManagementFactory;
//import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
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
        try {
            // CPU Usage as percentage
            String cpuSql = """
            SELECT ROUND(
                (SELECT value FROM v$sysstat 
                 WHERE name = 'CPU used by this session') /
                (SELECT value * 100 FROM v$parameter 
                 WHERE name = 'cpu_count'),
                2) as cpu_percentage
            FROM dual
        """;
            Double cpuUsage = jdbcTemplate.queryForObject(cpuSql, Double.class);
            metrics.setCpuUsagePercent(cpuUsage);

            // Memory Usage (SGA + PGA)
            String sgaSql = """
            SELECT ROUND(SUM(bytes)/(1024*1024), 2) as sga_mb 
            FROM v$sgastat
        """;
            Double sgaUsage = jdbcTemplate.queryForObject(sgaSql, Double.class);

            String pgaSql = """
            SELECT ROUND(value/(1024*1024), 2) as pga_mb 
            FROM v$pgastat 
            WHERE name = 'total PGA allocated'
        """;
            Double pgaUsage = jdbcTemplate.queryForObject(pgaSql, Double.class);

            metrics.setMemoryUsageMB(sgaUsage);
            metrics.setPgaUsageMB(pgaUsage);

            // Buffer Cache Hit Ratio
            String bufferCacheSql = """
            SELECT ROUND(
                (1 - (phy.value / (cur.value + con.value))) * 100,
                2) as buffer_cache_hit_ratio
            FROM v$sysstat cur, v$sysstat con, v$sysstat phy
            WHERE cur.name = 'db block gets'
            AND con.name = 'consistent gets'
            AND phy.name = 'physical reads'
        """;
            Double bufferCacheHitRatio = jdbcTemplate.queryForObject(bufferCacheSql, Double.class);
            metrics.setBufferCacheHitRatio(bufferCacheHitRatio);

            // IO Operations per Second
            String ioSql = """
            SELECT ROUND(
                value / 
                (SYSDATE - startup_time) * 86400,
                2) as io_per_second
            FROM v$sysstat, v$instance
            WHERE name = 'physical reads'
        """;
            Double ioRate = jdbcTemplate.queryForObject(ioSql, Double.class);
            metrics.setIoOperationsPerSecond(ioRate);

            metrics.setTimestamp(LocalDateTime.now());
            return metricsRepository.save(metrics);

        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException("Failed to collect performance metrics", e);
        }
    }


}

