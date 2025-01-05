package ma.fstt.springoracle.service;


import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.model.SlowQuery;
import ma.fstt.springoracle.repository.SlowQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceOptimizationServiceImpl implements PerformanceOptimizationService  {

    @Autowired
    private JdbcTemplate jdbcTemplate;

//    private final JdbcTemplate jdbcTemplate;
    private final SlowQueryRepository slowQueryRepository;
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizationService.class);

    // Get slow queries from the system
    @Transactional
    public List<SlowQuery> identifySlowQueries() {
        try {
            String sql = """
                SELECT sql_id, sql_text, elapsed_time, cpu_time, executions
                FROM v$sql
                WHERE elapsed_time > 1000000
                ORDER BY elapsed_time DESC
                FETCH FIRST 10 ROWS ONLY
            """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            for (Map<String, Object> row : results) {
                SlowQuery query = new SlowQuery();
                query.setSqlId((String) row.get("sql_id"));
                query.setSqlText((String) row.get("sql_text"));

                // Handle BigDecimal and other numeric types correctly
                query.setElapsedTime(((Number) row.get("elapsed_time")).doubleValue());
                query.setCpuTime(((Number) row.get("cpu_time")).doubleValue());

                // Convert executions to Integer safely
                Object executionsObj = row.get("executions");
                query.setExecutions(executionsObj != null ? ((Number) executionsObj).intValue() : 0);

                query.setCaptureTime(LocalDateTime.now());
                query.setStatus("IDENTIFIED");

                slowQueryRepository.save(query);
                logger.info("Identified slow query with SQL ID: " + query.getSqlId());
            }

            return slowQueryRepository.findByStatusOrderByElapsedTimeDesc("IDENTIFIED");

        } catch (Exception e) {
            logger.error("Error identifying slow queries: " + e.getMessage(), e);
            throw new RuntimeException("Error identifying slow queries", e);
        }
    }

    @Transactional
    public String optimizeQuery(Long queryId) {
        SlowQuery query = slowQueryRepository.findById(queryId)
                .orElseThrow(() -> new RuntimeException("Query not found"));

        String recommendations = null;

        try {
            recommendations = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
                try (CallableStatement dropTask = connection.prepareCall(
                        "BEGIN " +
                                "  BEGIN " +
                                "    DBMS_SQLTUNE.DROP_TUNING_TASK('TUNE_' || ?);" +
                                "  EXCEPTION " +
                                "    WHEN OTHERS THEN NULL;" +
                                "  END;" +
                                "END;"
                )) {
                    dropTask.setString(1, query.getSqlId());
                    dropTask.execute();
                }

                // Create tuning task with correct parameters
                try (CallableStatement createTask = connection.prepareCall(
                        "DECLARE " +
                                "  l_sql_tune_task_id VARCHAR2(100);" +
                                "BEGIN " +
                                "  l_sql_tune_task_id := DBMS_SQLTUNE.CREATE_TUNING_TASK(" +
                                "    sql_id => ?, " +
                                "    task_name => 'TUNE_' || ?, " +
                                "    time_limit => 3600" +
                                "  );" +
                                "END;"
                )) {
                    createTask.setString(1, query.getSqlId());
                    createTask.setString(2, query.getSqlId());
                    createTask.execute();
                }

                // Execute tuning task
                try (CallableStatement executeTask = connection.prepareCall(
                        "BEGIN " +
                                "  DBMS_SQLTUNE.EXECUTE_TUNING_TASK(" +
                                "    task_name => 'TUNE_' || ?" +
                                ");" +
                                "END;"
                )) {
                    executeTask.setString(1, query.getSqlId());
                    executeTask.execute();
                }

                // Retrieve recommendations
                try (CallableStatement getRecommendations = connection.prepareCall(
                        "BEGIN " +
                                "  ? := DBMS_SQLTUNE.REPORT_TUNING_TASK(" +
                                "    task_name => 'TUNE_' || ?, " +
                                "    type => 'TEXT', " +
                                "    level => 'TYPICAL'" +
                                ");" +
                                "END;"
                )) {
                    getRecommendations.registerOutParameter(1, Types.CLOB);
                    getRecommendations.setString(2, query.getSqlId());
                    getRecommendations.execute();

                    Clob clob = getRecommendations.getClob(1);
                    return clob != null ? clob.getSubString(1, (int) clob.length()) : null;
                }
            });

            logger.info("Optimization recommendations for query " + query.getSqlId() + ": " + recommendations);

        } catch (Exception e) {
            logger.error("Unexpected error during tuning for SQL ID: " + query.getSqlId(), e);
            throw new RuntimeException("Unexpected error during tuning: " + e.getMessage(), e);
        }

        query.setOptimizationRecommendations(recommendations);
        query.setStatus("OPTIMIZED");

        return slowQueryRepository.save(query).getOptimizationRecommendations();
    }
//    @Override
//    public Map<String, Object> getTuningRecommendations(String sqlId) {
//        String taskName = "TUNE_" + sqlId;
//        try {
//            // Create tuning task with proper bind variables
//            String createTask = """
//                DECLARE
//                  v_sql_id VARCHAR2(13);
//                  v_task_name VARCHAR2(30);
//                BEGIN
//                  v_sql_id := ?;
//                  v_task_name := ?;
//                  DBMS_SQLTUNE.CREATE_TUNING_TASK(
//                    sql_id => v_sql_id,
//                    task_name => v_task_name,
//                    time_limit => 3600
//                  );
//                END;
//            """;
//            jdbcTemplate.update(createTask, sqlId, taskName);
//
//            // Execute the tuning task
//            String executeTask = """
//                BEGIN
//                  DBMS_SQLTUNE.EXECUTE_TUNING_TASK(task_name => ?);
//                END;
//            """;
//            jdbcTemplate.update(executeTask, taskName);
//
//            // Get recommendations
//            String getRecommendations = """
//                SELECT DBMS_SQLTUNE.REPORT_TUNING_TASK(?) AS recommendations FROM dual
//            """;
//            return jdbcTemplate.queryForMap(getRecommendations, taskName);
//
//        } catch (Exception e) {
//            log.error("Error during SQL tuning for SQL_ID: " + sqlId, e);
//            Map<String, Object> errorResult = new HashMap<>();
//            errorResult.put("error", "Failed to get tuning recommendations: " + e.getMessage());
//            return errorResult;
//        } finally {
//            try {
//                // Clean up
//                String dropTask = """
//                    BEGIN
//                      DBMS_SQLTUNE.DROP_TUNING_TASK(?);
//                    END;
//                """;
//                jdbcTemplate.update(dropTask, taskName);
//            } catch (Exception e) {
//                log.error("Error dropping tuning task: " + taskName, e);
//            }
//        }
//    }
@Override
public Map<String, Object> getTuningRecommendations(String sqlId) {
    String taskName = "TUNE_" + sqlId;
    try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
        // Create tuning task
        try (CallableStatement stmt = connection.prepareCall(
                "{CALL DBMS_SQLTUNE.CREATE_TUNING_TASK(?, ?, 3600)}")) {
            stmt.setString(1, sqlId);
            stmt.setString(2, taskName);
            stmt.execute();
        }

        // Execute the tuning task
        try (CallableStatement stmt = connection.prepareCall(
                "{CALL DBMS_SQLTUNE.EXECUTE_TUNING_TASK(?)}")) {
            stmt.setString(1, taskName);
            stmt.execute();
        }

        // Get recommendations
        String reportSql = "SELECT DBMS_SQLTUNE.REPORT_TUNING_TASK(?) AS recommendations FROM dual";
        return jdbcTemplate.queryForMap(reportSql, taskName);

    } catch (Exception e) {
        log.error("Error during SQL tuning for SQL_ID: " + sqlId, e);
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", "Failed to get tuning recommendations: " + e.getMessage());
        return errorResult;
    } finally {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             CallableStatement stmt = connection.prepareCall(
                     "{CALL DBMS_SQLTUNE.DROP_TUNING_TASK(?)}")) {
            stmt.setString(1, taskName);
            stmt.execute();
        } catch (Exception e) {
            log.error("Error dropping tuning task: " + taskName, e);
        }
    }
}

    // Gather table statistics
    public void gatherTableStats(String schemaName, String tableName) {
        String sql = """
            BEGIN
                DBMS_STATS.GATHER_TABLE_STATS(
                    ownname => ?,
                    tabname => ?,
                    estimate_percent => DBMS_STATS.AUTO_SAMPLE_SIZE,
                    method_opt => 'FOR ALL COLUMNS SIZE AUTO',
                    cascade => TRUE
                );
            END;
        """;
        jdbcTemplate.update(sql, schemaName, tableName);
    }

    // Schedule statistics gathering job
    public void scheduleStatsGathering(String schemaName) {
        String sql = """
            BEGIN
                DBMS_SCHEDULER.CREATE_JOB(
                    job_name => 'GATHER_STATS_' || ?,
                    job_type => 'PLSQL_BLOCK',
                    job_action => 'BEGIN DBMS_STATS.GATHER_SCHEMA_STATS(''' || ? || '''); END;',
                    repeat_interval => 'FREQ=DAILY;BYHOUR=2',  -- Run daily at 2 AM
                    enabled => TRUE
                );
            END;
        """;
        jdbcTemplate.update(sql, schemaName, schemaName);
    }
}