package ma.fstt.springoracle.service;

import ma.fstt.springoracle.dto.*;

public interface OracleHAService {
    DataGuardStatusDTO getDataGuardStatus();

    void configureDataGuard(DataGuardConfigDTO config);

    HaOperationResponseDTO simulateFailover();

    HaOperationResponseDTO simulateSwitchback();

    AvailabilityReportDTO generateAvailabilityReport(String startDate, String endDate);
}