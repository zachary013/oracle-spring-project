package ma.fstt.springoracle.service;

import jakarta.annotation.PostConstruct;
import ma.fstt.springoracle.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

@Service
public class OracleHAServiceImpl implements OracleHAService {
    private static final Logger logger = LoggerFactory.getLogger(OracleHAServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        createOperationsLogTable();
    }

    private void createOperationsLogTable() {
        try {
            String createTableSql = """
                CREATE TABLE ha_operations_log (
                    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    type VARCHAR2(20),
                    operation_date TIMESTAMP,
                    success NUMBER(1),
                    execution_time NUMBER,
                    error_message VARCHAR2(4000)
                )
            """;
            jdbcTemplate.execute(createTableSql);
        } catch (Exception e) {
            logger.warn("Operations log table might already exist: " + e.getMessage());
        }
    }

    @Override
    public DataGuardStatusDTO getDataGuardStatus() {
        try {
            String sql = """
                SELECT 
                    d.db_unique_name,
                    d.database_role,
                    d.protection_mode,
                    d.protection_level,
                    d.switchover_status,
                    d.open_mode,
                    dg.facility,
                    dg.severity,
                    dg.error_code,
                    dg.status,
                    dg.timestamp,
                    dg.gap_status
                FROM v$database d
                LEFT JOIN v$dataguard_status dg ON 1=1
                WHERE ROWNUM = 1
                ORDER BY dg.timestamp DESC
            """;

            Map<String, Object> result = jdbcTemplate.queryForMap(sql);

            return DataGuardStatusDTO.builder()
                    .databaseRole((String) result.get("DATABASE_ROLE"))
                    .protectionMode((String) result.get("PROTECTION_MODE"))
                    .protectionLevel((String) result.get("PROTECTION_LEVEL"))
                    .switchoverStatus((String) result.get("SWITCHOVER_STATUS"))
                    .status((String) result.get("STATUS"))
                    .gapStatus((String) result.get("GAP_STATUS"))
                    .timestamp(new Date())
                    .statusCode("SUCCESS")
                    .build();
        } catch (Exception e) {
            logger.error("Error getting Data Guard status: " + e.getMessage(), e);
            return DataGuardStatusDTO.builder()
                    .statusCode("ERROR")
                    .errorMessage(e.getMessage())
                    .timestamp(new Date())
                    .build();
        }
    }

    @Override
    @Transactional
    public void configureDataGuard(DataGuardConfigDTO config) {
        validateConfig(config);

        try {
            // Verify SYSDBA connection
            verifySysConnection(config);

            // Configure primary database
            configurePrimaryDatabase(config);

            // Configure standby database
            configureStandbyDatabase(config);

            // Initialize Data Guard broker configuration
            initializeDataGuardBroker(config);

            logger.info("Data Guard configuration completed successfully");
        } catch (Exception e) {
            logger.error("Error configuring Data Guard: " + e.getMessage(), e);
            throw new RuntimeException("Failed to configure Data Guard: " + e.getMessage());
        }
    }

    private void validateConfig(DataGuardConfigDTO config) {
        if (config.getPrimaryHost() == null || config.getPrimaryHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Primary host cannot be empty");
        }
        if (config.getStandbyHost() == null || config.getStandbyHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Standby host cannot be empty");
        }
        if (config.getPrimaryPort() <= 0 || config.getPrimaryPort() > 65535) {
            throw new IllegalArgumentException("Invalid primary port number");
        }
        if (config.getStandbyPort() <= 0 || config.getStandbyPort() > 65535) {
            throw new IllegalArgumentException("Invalid standby port number");
        }
        if (config.getPrimaryDbName() == null || config.getPrimaryDbName().trim().isEmpty()) {
            throw new IllegalArgumentException("Primary database name cannot be empty");
        }
        if (config.getStandbyDbName() == null || config.getStandbyDbName().trim().isEmpty()) {
            throw new IllegalArgumentException("Standby database name cannot be empty");
        }
    }

    private void verifySysConnection(DataGuardConfigDTO config) throws SQLException {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException("Cannot establish SYSDBA connection");
            }
        }
    }

    private void configurePrimaryDatabase(DataGuardConfigDTO config) {
        try {
            String sql = """
                BEGIN
                    -- Enable force logging
                    EXECUTE IMMEDIATE 'ALTER DATABASE FORCE LOGGING';
                    
                    -- Enable flashback database
                    EXECUTE IMMEDIATE 'ALTER DATABASE FLASHBACK ON';
                    
                    -- Set DB_UNIQUE_NAME parameter
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET DB_UNIQUE_NAME=''%s'' SCOPE=SPFILE';
                    
                    -- Configure DG_CONFIG
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET LOG_ARCHIVE_CONFIG=''DG_CONFIG=(%s,%s)'' SCOPE=BOTH';
                    
                    -- Configure archive destination
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET LOG_ARCHIVE_DEST_1=''LOCATION=USE_DB_RECOVERY_FILE_DEST VALID_FOR=(ALL_LOGFILES,ALL_ROLES) DB_UNIQUE_NAME=%s'' SCOPE=BOTH';
                    
                    -- Configure standby archive destination
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET LOG_ARCHIVE_DEST_2=''SERVICE=%s ASYNC VALID_FOR=(ONLINE_LOGFILES,PRIMARY_ROLE) DB_UNIQUE_NAME=%s'' SCOPE=BOTH';
                    
                    -- Set STANDBY_FILE_MANAGEMENT
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET STANDBY_FILE_MANAGEMENT=AUTO SCOPE=BOTH';
                    
                    -- Enable Data Guard Broker
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET DG_BROKER_START=TRUE SCOPE=BOTH';
                END;
                """;

            jdbcTemplate.execute(String.format(sql,
                    config.getPrimaryDbName(),
                    config.getPrimaryDbName(),
                    config.getStandbyDbName(),
                    config.getPrimaryDbName(),
                    config.getStandbyDbName(),
                    config.getStandbyDbName()));

        } catch (Exception e) {
            logger.error("Error configuring primary database: " + e.getMessage(), e);
            throw new RuntimeException("Failed to configure primary database: " + e.getMessage());
        }
    }

    private void configureStandbyDatabase(DataGuardConfigDTO config) {
        try {
            String sql = """
                BEGIN
                    -- Enable force logging
                    EXECUTE IMMEDIATE 'ALTER DATABASE FORCE LOGGING';
                    
                    -- Set DB_UNIQUE_NAME parameter
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET DB_UNIQUE_NAME=''%s'' SCOPE=SPFILE';
                    
                    -- Configure DG_CONFIG
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET LOG_ARCHIVE_CONFIG=''DG_CONFIG=(%s,%s)'' SCOPE=BOTH';
                    
                    -- Configure archive destination for standby
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET LOG_ARCHIVE_DEST_1=''LOCATION=USE_DB_RECOVERY_FILE_DEST VALID_FOR=(ALL_LOGFILES,ALL_ROLES) DB_UNIQUE_NAME=%s'' SCOPE=BOTH';
                    
                    -- Configure archive destination for primary
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET LOG_ARCHIVE_DEST_2=''SERVICE=%s ASYNC VALID_FOR=(ONLINE_LOGFILES,PRIMARY_ROLE) DB_UNIQUE_NAME=%s'' SCOPE=BOTH';
                    
                    -- Set FAL_SERVER parameter
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET FAL_SERVER=''%s'' SCOPE=BOTH';
                    
                    -- Set FAL_CLIENT parameter
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET FAL_CLIENT=''%s'' SCOPE=BOTH';
                    
                    -- Set STANDBY_FILE_MANAGEMENT
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET STANDBY_FILE_MANAGEMENT=AUTO SCOPE=BOTH';
                    
                    -- Enable Data Guard Broker
                    EXECUTE IMMEDIATE 'ALTER SYSTEM SET DG_BROKER_START=TRUE SCOPE=BOTH';
                    
                    -- Start managed recovery
                    EXECUTE IMMEDIATE 'ALTER DATABASE RECOVER MANAGED STANDBY DATABASE DISCONNECT FROM SESSION';
                END;
                """;

            jdbcTemplate.execute(String.format(sql,
                    config.getStandbyDbName(),
                    config.getPrimaryDbName(),
                    config.getStandbyDbName(),
                    config.getStandbyDbName(),
                    config.getPrimaryDbName(),
                    config.getPrimaryDbName(),
                    config.getPrimaryDbName(),
                    config.getStandbyDbName()));

        } catch (Exception e) {
            logger.error("Error configuring standby database: " + e.getMessage(), e);
            throw new RuntimeException("Failed to configure standby database: " + e.getMessage());
        }
    }

    private void initializeDataGuardBroker(DataGuardConfigDTO config) {
        try {
            String sql = """
                BEGIN
                    -- Create broker configuration
                    DBMS_DG.INIT_CONFIGURATION(
                        primary_db_name => ?,
                        standby_db_name => ?
                    );
                    
                    -- Add configuration details
                    DBMS_DG.ADD_CONFIGURATION(
                        primary_system => ?,
                        primary_port => ?,
                        standby_system => ?,
                        standby_port => ?,
                        net_server_name => 'LISTENER'
                    );
                    
                    -- Enable the configuration
                    DBMS_DG.ENABLE_CONFIGURATION();
                END;
                """;

            jdbcTemplate.update(sql,
                    config.getPrimaryDbName(),
                    config.getStandbyDbName(),
                    config.getPrimaryHost(),
                    config.getPrimaryPort(),
                    config.getStandbyHost(),
                    config.getStandbyPort());

        } catch (Exception e) {
            logger.error("Error initializing Data Guard broker: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Data Guard broker: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public HaOperationResponseDTO simulateFailover() {
        try {
            long startTime = System.currentTimeMillis();

            verifyFailoverPrerequisites();

            String sql = """
                BEGIN
                    -- Prepare for failover
                    DBMS_DG.PREPARE_FOR_FAILOVER();
                    
                    -- Initiate failover
                    DBMS_DG.INITIATE_FAILOVER(
                        immediate => TRUE,
                        force => FALSE,
                        target_role => 'PRIMARY'
                    );
                END;
                """;

            jdbcTemplate.execute(sql);

            long executionTime = System.currentTimeMillis() - startTime;
            logOperation("FAILOVER", true, executionTime, null);

            return HaOperationResponseDTO.builder()
                    .success(true)
                    .executionTime(executionTime)
                    .message("Failover simulation completed successfully")
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            logOperation("FAILOVER", false, 0L, e.getMessage());
            logger.error("Failover simulation failed: " + e.getMessage(), e);
            return HaOperationResponseDTO.builder()
                    .success(false)
                    .message("Failover simulation failed: " + e.getMessage())
                    .timestamp(new Date())
                    .build();
        }
    }

    @Override
    @Transactional
    public HaOperationResponseDTO simulateSwitchback() {
        try {
            long startTime = System.currentTimeMillis();

            verifySwitchbackPrerequisites();

            String sql = """
                BEGIN
                    DBMS_DG.INITIATE_SWITCHOVER(
                        from_database => ?,
                        to_database => ?,
                        force => FALSE
                    );
                END;
                """;

            jdbcTemplate.execute(sql);

            long executionTime = System.currentTimeMillis() - startTime;
            logOperation("SWITCHBACK", true, executionTime, null);

            return HaOperationResponseDTO.builder()
                    .success(true)
                    .executionTime(executionTime)
                    .message("Switchback simulation completed successfully")
                    .timestamp(new Date())
                    .build();
        } catch (Exception e) {
            logOperation("SWITCHBACK", false, 0L, e.getMessage());
            logger.error("Switchback simulation failed: " + e.getMessage(), e);
            return HaOperationResponseDTO.builder()
                    .success(false)
                    .message("Switchback simulation failed: " + e.getMessage())
                    .timestamp(new Date())
                    .build();
        }
    }

    private void verifyFailoverPrerequisites() {
        String sql = """
            SELECT database_role, switchover_status 
            FROM v$database
            """;

        Map<String, Object> status = jdbcTemplate.queryForMap(sql);

        if (!"PHYSICAL STANDBY".equals(status.get("DATABASE_ROLE"))) {
            throw new IllegalStateException("Database must be in PHYSICAL STANDBY role for failover");
        }

        if (!"TO PRIMARY".equals(status.get("SWITCHOVER_STATUS"))) {
            throw new IllegalStateException("Database not ready for failover. Current status: " + status.get("SWITCHOVER_STATUS"));
        }
    }

    private void verifySwitchbackPrerequisites() {
        String sql = """
            SELECT database_role, switchover_status 
            FROM v$database
            """;

        Map<String, Object> status = jdbcTemplate.queryForMap(sql);

        if (!"PRIMARY".equals(status.get("DATABASE_ROLE"))) {
            throw new IllegalStateException("Database must be in PRIMARY role for switchback");
        }

        if (!"TO STANDBY".equals(status.get("SWITCHOVER_STATUS"))) {
            throw new IllegalStateException("Database not ready for switchback. Current status: " + status.get("SWITCHOVER_STATUS"));
        }
    }

    @Override
    public AvailabilityReportDTO generateAvailabilityReport(String startDate, String endDate) {
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total_simulations,
                    COUNT(CASE WHEN type = 'FAILOVER' AND success = 1 THEN 1 END) as successful_failovers,
                    COUNT(CASE WHEN type = 'SWITCHBACK' AND success = 1 THEN 1 END) as successful_switchbacks,
                    AVG(CASE WHEN type = 'FAILOVER' THEN execution_time END) as avg_failover_time,
                    AVG(CASE WHEN type = 'SWITCHBACK' THEN execution_time END) as avg_switchback_time
                FROM ha_operations_log
                WHERE operation_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')
            """;

            return jdbcTemplate.queryForObject(sql,
                    (rs, rowNum) -> {
                        int total = rs.getInt("total_simulations");
                        int successFailovers = rs.getInt("successful_failovers");
                        int successSwitchbacks = rs.getInt("successful_switchbacks");

                        return AvailabilityReportDTO.builder()
                                .totalSimulations(total)
                                .successfulFailovers(successFailovers)
                                .successfulSwitchbacks(successSwitchbacks)
                                .avgFailoverTimeMs(rs.getDouble("avg_failover_time"))
                                .avgSwitchbackTimeMs(rs.getDouble("avg_switchback_time"))
                                .failoverSuccessRate(calculateRate(successFailovers, total))
                                .switchbackSuccessRate(calculateRate(successSwitchbacks, total))
                                .startDate(startDate)
                                .endDate(endDate)
                                .build();
                    },
                    startDate, endDate);
        } catch (Exception e) {
            logger.error("Error generating availability report: " + e.getMessage(), e);
            return AvailabilityReportDTO.builder()
                    .error("Failed to generate report: " + e.getMessage())
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
        }
    }

    private double calculateRate(int success, int total) {
        return total > 0 ? (success * 100.0 / total) : 0.0;
    }

    private void logOperation(String type, boolean success, Long executionTime, String errorMessage) {
        String sql = """
            INSERT INTO ha_operations_log 
            (type, operation_date, success, execution_time, error_message)
            VALUES (?, SYSTIMESTAMP, ?, ?, ?)
            """;

        jdbcTemplate.update(sql, type, success ? 1 : 0, executionTime, errorMessage);
    }
}
