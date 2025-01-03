package ma.fstt.springoracle.controller;

import ma.fstt.springoracle.model.PerformanceMetrics;
import ma.fstt.springoracle.service.PerformanceMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceMonitoringController {
    @Autowired
    private PerformanceMonitoringService monitoringService;

//    @GetMapping("/awr")
//    public ResponseEntity<Map<String, Object>> getAWRReport(
//            @RequestParam LocalDateTime start,
//            @RequestParam LocalDateTime end) {
//        return ResponseEntity.ok(monitoringService.getAWRReport(start, end));
//    }

    @GetMapping("/awr")
    public ResponseEntity<List<Map<String, Object>>> getAWRReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        try {
            return ResponseEntity.ok(monitoringService.getAWRReport(start, end));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/ash")
    public ResponseEntity<List<Map<String, Object>>> getASHReport() {
        return ResponseEntity.ok(monitoringService.getASHReport());
    }

    @GetMapping("/metrics")
    public ResponseEntity<PerformanceMetrics> getRealTimeMetrics() {
        return ResponseEntity.ok(monitoringService.getRealTimeMetrics());
    }

}
