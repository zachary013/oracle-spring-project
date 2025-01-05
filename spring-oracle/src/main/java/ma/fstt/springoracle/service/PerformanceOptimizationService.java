package ma.fstt.springoracle.service;

import ma.fstt.springoracle.model.SlowQuery;

import java.util.List;
import java.util.Map;

public interface PerformanceOptimizationService {

    public Map<String, Object> getTuningRecommendations(String sqlId) ;

    public void gatherTableStats(String schemaName, String tableName) ;

    public void scheduleStatsGathering(String schemaName) ;

    public List<SlowQuery> identifySlowQueries() ;

    public String optimizeQuery(Long queryId) ;

}
