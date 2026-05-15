package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshotEntity, Long> {

    Optional<AnalyticsSnapshotEntity> findTopByCandidateIdOrderByObservedAtDesc(String candidateId);

    List<AnalyticsSnapshotEntity> findTop10BySymbolOrderByObservedAtDesc(String symbol);

    /** All snapshots for a specific regime label observed after the given timestamp. */
    List<AnalyticsSnapshotEntity> findByRegimeLabelAndObservedAtAfterOrderByObservedAtDesc(
            String regimeLabel,
            Instant since
    );

    /** Snapshots with deployment confidence above the given threshold, newest-first. */
    List<AnalyticsSnapshotEntity> findByDeploymentConfidenceScoreGreaterThanOrderByDeploymentConfidenceScoreDesc(
            double minScore
    );
}
