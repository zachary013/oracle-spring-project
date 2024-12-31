package ma.fstt.springoracle.controller;

import ma.fstt.springoracle.model.SlowQuery;
import ma.fstt.springoracle.service.SqlTuningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slow-queries")
public class SlowQueryController {

    @Autowired
    private SqlTuningService sqlTuningService;

    @GetMapping
    public List<SlowQuery> getSlowQueries() {
        return sqlTuningService.getAllSlowQueries();
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeQuery(@RequestBody String queryText) {
        try {
            // Nettoyer la chaîne : retirer les guillemets doubles (s'il y en a)
            String cleanedQueryText = queryText.trim().replaceAll("^\"|\"$", "");

            // Appeler le service avec la chaîne nettoyée
            String report = sqlTuningService.analyzeQuery(cleanedQueryText);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            // Retourner un message d'erreur clair
            return ResponseEntity.status(500).body("Error during SQL analysis: " + e.getMessage());
        }
    }
}
