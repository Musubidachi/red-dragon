package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ValidationVerdictRepository extends JpaRepository<ValidationVerdictEntity, Long> {

    List<ValidationVerdictEntity> findTop25ByCandidateIdOrderByCreatedAtDesc(String candidateId);

    List<ValidationVerdictEntity> findTop25BySymbolOrderByCreatedAtDesc(String symbol);

    /**
     * All verdicts matching the given decisions since the given timestamp, newest-first.
     * Use for raw history queries.
     */
    List<ValidationVerdictEntity> findByVerdictInAndCreatedAtAfterOrderByScoreDescCreatedAtDesc(
            List<String> verdicts,
            Instant since
    );

    /**
     * Most recent verdict per candidate since the given timestamp, ordered by score descending.
     * Deduplicates candidates that have been run through the pipeline more than once.
     */
    @Query("""
            SELECT v FROM ValidationVerdictEntity v
            WHERE v.verdict IN :verdicts
              AND v.createdAt > :since
              AND v.id = (
                  SELECT MAX(v2.id) FROM ValidationVerdictEntity v2
                  WHERE v2.candidateId = v.candidateId
              )
            ORDER BY v.score DESC, v.createdAt DESC
            """)
    List<ValidationVerdictEntity> findLatestPerCandidateSince(
            @Param("verdicts") List<String> verdicts,
            @Param("since") Instant since
    );

    /** Count verdicts with a given decision string after the specified timestamp. */
    long countByVerdictAndCreatedAtAfter(String verdict, Instant since);

    /** All verdicts with the specified deployment tiers since the given timestamp, newest-first. */
    List<ValidationVerdictEntity> findByDeploymentTierInAndCreatedAtAfterOrderByCreatedAtDesc(
            List<String> deploymentTiers,
            Instant since
    );

    /** The single highest-scoring verdict ever recorded for a symbol. */
    ValidationVerdictEntity findTopBySymbolOrderByScoreDesc(String symbol);

    /** Verdict history after the provided timestamp, newest-first. */
    List<ValidationVerdictEntity> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since);

}
