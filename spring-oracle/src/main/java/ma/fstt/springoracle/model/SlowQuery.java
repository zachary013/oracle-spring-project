package ma.fstt.springoracle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "slow_queries")
public class SlowQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", length = 4000)
    private String queryText;

    @Column(name = "execution_time")
    private Long executionTime;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    private String executionPlan;
    // Ajouté pour stocker le plan d'exécution
    private String tuningReport;



}

