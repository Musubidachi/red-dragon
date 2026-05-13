package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.BacktestResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BacktestResultRepository extends JpaRepository<BacktestResultEntity, Long> {

    List<BacktestResultEntity> findByRunIdOrderByScoreDesc(String runId);

    boolean existsByRunId(String runId);

    /** All results for a given strategy name, highest score first. */
    List<BacktestResultEntity> findByStrategyNameOrderByScoreDesc(String strategyName);

    /** Distinct run IDs available in the store, ordered by most recent first. */
    @Query("SELECT DISTINCT b.runId FROM BacktestResultEntity b ORDER BY b.runId DESC")
    List<String> findDistinctRunIds();

    /** Count results with a given verdict for a specific run. */
    long countByVerdictAndRunId(String verdict, String runId);
}
