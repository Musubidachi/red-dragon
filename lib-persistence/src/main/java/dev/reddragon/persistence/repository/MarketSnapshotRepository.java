package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.MarketSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshotEntity, Long> {

    Optional<MarketSnapshotEntity> findTopByCandidateIdOrderByObservedAtDesc(String candidateId);

    List<MarketSnapshotEntity> findTop10BySymbolOrderByObservedAtDesc(String symbol);

    /** All snapshots for a symbol observed after the given timestamp, newest-first. */
    List<MarketSnapshotEntity> findBySymbolAndObservedAtAfterOrderByObservedAtDesc(
            String symbol,
            Instant since
    );
}
