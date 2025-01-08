package ma.fstt.springoracle.controller;

import ma.fstt.springoracle.dto.*;
import ma.fstt.springoracle.service.OracleHAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ha")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class OracleHAController {

    @Autowired
    private OracleHAService oracleHAService;

    @GetMapping("/status")
    public ResponseEntity<DataGuardStatusDTO> getDataGuardStatus() {
        DataGuardStatusDTO status = oracleHAService.getDataGuardStatus();
        return ResponseEntity.ok(status);
    }

    @PostMapping("/configure")
    public ResponseEntity<?> configureDataGuard(@RequestBody DataGuardConfigDTO config) {
        try {
            oracleHAService.configureDataGuard(config);
            return ResponseEntity.ok(Map.of(
                    "message", "Data Guard configured successfully",
                    "status", "SUCCESS"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Configuration failed: " + e.getMessage(),
                    "status", "ERROR"
            ));
        }
    }

    @PostMapping("/simulate/failover")
    public ResponseEntity<HaOperationResponseDTO> simulateFailover() {
        HaOperationResponseDTO response = oracleHAService.simulateFailover();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate/switchback")
    public ResponseEntity<HaOperationResponseDTO> simulateSwitchback() {
        HaOperationResponseDTO response = oracleHAService.simulateSwitchback();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report")
    public ResponseEntity<AvailabilityReportDTO> getAvailabilityReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        AvailabilityReportDTO report = oracleHAService.generateAvailabilityReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.badRequest().body(Map.of(
                "message", e.getMessage(),
                "status", "ERROR"
        ));
    }
}