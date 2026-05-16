package dev.reddragon.persistence.services.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.MarketQuoteObservationEntity;

public interface MarketQuoteObservationRepository extends JpaRepository<MarketQuoteObservationEntity, Long> {

    List<MarketQuoteObservationEntity> findBySymbolAndObservedAtAfterOrderByObservedAtDesc(
            String symbol,
            Instant since
    );
}
