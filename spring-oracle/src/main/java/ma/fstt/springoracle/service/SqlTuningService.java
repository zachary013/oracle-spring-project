package ma.fstt.springoracle.service;


import jakarta.transaction.Transactional;
import ma.fstt.springoracle.model.SlowQuery;
import ma.fstt.springoracle.repository.SlowQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SqlTuningService {

    @Autowired
    private SlowQueryRepository slowQueryRepository;

    @Transactional
    public String analyzeQuery(String queryText) {
        try {
            // Pour simplifier, vous pouvez soit appeler des outils Oracle pour analyser la requête
            // Soit implémenter votre propre logique de tuning (par exemple, avec AWR/ASH, EXPLAIN PLAN, etc.)
            String report = generateSqlTuningReport(queryText);

            // Enregistrer la requête dans la base de données si elle est considérée comme lente
            saveSlowQuery(queryText, report);

            return report;

        } catch (Exception e) {
            // En cas d'erreur, lancer une exception spécifique
            throw new RuntimeException("Error during SQL analysis: " + e.getMessage(), e);
        }
    }

    // Générer un rapport de tuning pour la requête SQL (vous pouvez personnaliser cela selon votre logique)
    private String generateSqlTuningReport(String queryText) {
        // Simuler un rapport de tuning (remplacez par votre logique réelle)
        // Par exemple, vous pouvez appeler un package Oracle pour exécuter le SQL Tuning Advisor

        // Exemple de rapport simplifié
        return "SQL Tuning Report for query: " + queryText + "\n" +
                "Execution time: High\n" +
                "Recommended tuning: Add index on column X";
    }

    // Enregistrer une requête lente dans la base de données
    private void saveSlowQuery(String queryText, String tuningReport) {
        SlowQuery slowQuery = new SlowQuery();
        slowQuery.setQueryText(queryText);
        slowQuery.setExecutionTime(System.currentTimeMillis()); // Exemple de calcul d'exécution
        slowQuery.setLastExecutedAt(java.time.LocalDateTime.now());

        // Vous pouvez ajouter des critères pour enregistrer la requête en fonction du temps d'exécution
        slowQueryRepository.save(slowQuery);
    }

    // Récupérer toutes les requêtes lentes enregistrées
    public List<SlowQuery> getAllSlowQueries() {
        return slowQueryRepository.findAll();
    }
}