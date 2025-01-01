package ma.fstt.springoracle.service;

import ma.fstt.springoracle.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class OracleHAServiceImpl implements OracleHAService {
    private static final Logger logger = LoggerFactory.getLogger(OracleHAServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public DataGuardStatusDTO getDataGuardStatus() {
        try {
            String sql = """
                SELECT 
                    d.database_role,
                    d.protection_mode,
                    d.protection_level,
                    d.switchover_status,
                    s.status,
                    s.gap_status
                FROM v$database d, v$dataguard_status s
                WHERE ROWNUM = 1
            """;

            return jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    return DataGuardStatusDTO.builder()
                            .databaseRole(rs.getString("database_role"))
                            .protectionMode(rs.getString("protection_mode"))
                            .protectionLevel(rs.getString("protection_level"))
                            .switchoverStatus(rs.getString("switchover_status"))
                            .status(rs.getString("status"))
                            .gapStatus(rs.getString("gap_status"))
                            .timestamp(new Date())
                            .statusCode("SUCCESS")
                            .build();
                }
                return null;
            });

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
    public void configureDataGuard(DataGuardConfigDTO config) {
        try {
            validateConfig(config);

            String configurePrimarySql = """
                BEGIN
                    DBMS_DG.INITIATE_FS_FAILOVER(
                        primary_db_name => ?,
                        standby_db_name => ?,
                        primary_host => ?,
                        primary_port => ?,
                        standby_host => ?,
                        standby_port => ?
                    );
                END;
            """;

            jdbcTemplate.update(configurePrimarySql,
                    "PRIMARY_DB", "STANDBY_DB",
                    config.getPrimaryHost(), config.getPrimaryPort(),
                    config.getStandbyHost(), config.getStandbyPort());

            logger.info("Data Guard configuration completed successfully");

        } catch (Exception e) {
            logger.error("Error configuring Data Guard: " + e.getMessage(), e);
            throw new RuntimeException("Failed to configure Data Guard: " + e.getMessage());
        }
    }

    @Override
    public HaOperationResponseDTO simulateFailover() {
        try {
            logger.info("Starting failover simulation");
            long startTime = System.currentTimeMillis();

            simulateNetworkPartition();

            String failoverSql = """
                BEGIN
                    DBMS_DG.SIMULATE_FAILOVER(
                        failover_type => 'IMMEDIATE',
                        target_role => 'PRIMARY'
                    );
                END;
            """;

            jdbcTemplate.execute(failoverSql);

            long executionTime = System.currentTimeMillis() - startTime;

            logSimulationResult("FAILOVER", executionTime, true);

            return HaOperationResponseDTO.builder()
                    .success(true)
                    .executionTime(executionTime)
                    .message("Failover simulation completed successfully")
                    .timestamp(new Date())
                    .build();

        } catch (Exception e) {
            logger.error("Failover simulation failed: " + e.getMessage(), e);
            return HaOperationResponseDTO.builder()
                    .success(false)
                    .message("Failover simulation failed: " + e.getMessage())
                    .timestamp(new Date())
                    .build();
        }
    }

    @Override
    public HaOperationResponseDTO simulateSwitchback() {
        try {
            logger.info("Starting switchback simulation");
            long startTime = System.currentTimeMillis();

            String switchbackSql = """
                BEGIN
                    DBMS_DG.SIMULATE_SWITCHOVER(
                        target_role => 'PRIMARY',
                        wait_for_completion => TRUE
                    );
                END;
            """;

            jdbcTemplate.execute(switchbackSql);

            long executionTime = System.currentTimeMillis() - startTime;

            logSimulationResult("SWITCHBACK", executionTime, true);

            return HaOperationResponseDTO.builder()
                    .success(true)
                    .executionTime(executionTime)
                    .message("Switchback simulation completed successfully")
                    .timestamp(new Date())
                    .build();

        } catch (Exception e) {
            logger.error("Switchback simulation failed: " + e.getMessage(), e);
            return HaOperationResponseDTO.builder()
                    .success(false)
                    .message("Switchback simulation failed: " + e.getMessage())
                    .timestamp(new Date())
                    .build();
        }
    }

    @Override
    public AvailabilityReportDTO generateAvailabilityReport(String startDate, String endDate) {
        try {
            String sql = """
                SELECT 
                    COUNT(*) as total_simulations,
                    AVG(CASE 
                        WHEN simulation_type = 'FAILOVER' THEN execution_time 
                    END) as avg_failover_time,
                    AVG(CASE 
                        WHEN simulation_type = 'SWITCHBACK' THEN execution_time 
                    END) as avg_switchback_time,
                    COUNT(CASE 
                        WHEN simulation_type = 'FAILOVER' AND status = 'SUCCESS' 
                        THEN 1 
                    END) as successful_failovers,
                    COUNT(CASE 
                        WHEN simulation_type = 'SWITCHBACK' AND status = 'SUCCESS' 
                        THEN 1 
                    END) as successful_switchbacks
                FROM ha_simulation_log
                WHERE simulation_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') 
                AND TO_DATE(?, 'YYYY-MM-DD')
            """;

            return jdbcTemplate.query(sql, new Object[]{startDate, endDate}, rs -> {
                if (rs.next()) {
                    int totalSimulations = rs.getInt("total_simulations");
                    int successfulFailovers = rs.getInt("successful_failovers");
                    int successfulSwitchbacks = rs.getInt("successful_switchbacks");

                    double failoverSuccessRate = totalSimulations > 0
                            ? (successfulFailovers * 100.0 / totalSimulations)
                            : 0.0;
                    double switchbackSuccessRate = totalSimulations > 0
                            ? (successfulSwitchbacks * 100.0 / totalSimulations)
                            : 0.0;

                    return AvailabilityReportDTO.builder()
                            .totalSimulations(totalSimulations)
                            .avgFailoverTimeMs(rs.getDouble("avg_failover_time"))
                            .avgSwitchbackTimeMs(rs.getDouble("avg_switchback_time"))
                            .successfulFailovers(successfulFailovers)
                            .successfulSwitchbacks(successfulSwitchbacks)
                            .failoverSuccessRate(Math.round(failoverSuccessRate * 100.0) / 100.0)
                            .switchbackSuccessRate(Math.round(switchbackSuccessRate * 100.0) / 100.0)
                            .startDate(startDate)
                            .endDate(endDate)
                            .build();
                }
                return null;
            });

        } catch (Exception e) {
            logger.error("Error generating availability report: " + e.getMessage(), e);
            return AvailabilityReportDTO.builder()
                    .error(e.getMessage())
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();
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
    }

    private void simulateNetworkPartition() {
        logger.info("Simulating network partition between primary and standby");
        try {
            Thread.sleep(2000); // Simulate 2-second network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logSimulationResult(String simulationType, long executionTime, boolean success) {
        String sql = """
            INSERT INTO ha_simulation_log 
            (simulation_type, execution_time, status, simulation_date)
            VALUES (?, ?, ?, SYSDATE)
        """;

        jdbcTemplate.update(sql, simulationType, executionTime,
                success ? "SUCCESS" : "FAILURE");
    }
}