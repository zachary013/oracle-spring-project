package ma.fstt.springoracle.controller;


import lombok.RequiredArgsConstructor;
import ma.fstt.springoracle.model.SlowQuery;
import ma.fstt.springoracle.service.PerformanceOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
//import com.oracle.admin.service.PerformanceOptimizationService;
import java.util.*;

@RestController
@RequestMapping("/api/optimization")
@RequiredArgsConstructor
public class PerformanceOptimizationController {
    @Autowired
    private final PerformanceOptimizationService performanceOptimizationService;

    @GetMapping("/slowQueries")
    public ResponseEntity<List<SlowQuery>> GetSlowQueries() {
        return ResponseEntity.ok(performanceOptimizationService.identifySlowQueries());
    }


    @PostMapping("/optimize-query/{queryId}")
    public ResponseEntity<String> optimizeQuery(@PathVariable Long queryId) {
        return ResponseEntity.ok(performanceOptimizationService.optimizeQuery(queryId));
    }

    @PostMapping("/gather-stats")
    public ResponseEntity<Void> gatherTableStats(
            @RequestParam String schemaName,
            @RequestParam String tableName) {
        performanceOptimizationService.gatherTableStats(schemaName, tableName);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/schedule-stats")
    public ResponseEntity<Void> scheduleStatsGathering(
            @RequestParam String schemaName) {
        performanceOptimizationService.scheduleStatsGathering(schemaName);
        return ResponseEntity.ok().build();
    }
}
