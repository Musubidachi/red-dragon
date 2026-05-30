package dev.reddragon.persistence.services.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.MarketStateSnapshotEntity;

public interface MarketStateSnapshotRepository extends JpaRepository<MarketStateSnapshotEntity, Long> {

    List<MarketStateSnapshotEntity> findAllByOrderByObservedAtDesc(Pageable pageable);

    List<MarketStateSnapshotEntity> findBySymbolOrderByObservedAtDesc(String symbol, Pageable pageable);
}
