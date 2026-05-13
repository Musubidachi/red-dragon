package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.MarketBarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MarketBarRepository extends JpaRepository<MarketBarEntity, Long> {

    List<MarketBarEntity> findBySymbolAndBarDateBetweenOrderByBarDateAsc(
            String symbol,
            LocalDate from,
            LocalDate to
    );

    List<MarketBarEntity> findTop60BySymbolOrderByBarDateDesc(String symbol);

    /** Deletes stale bar data older than the given cutoff date for a symbol. */
    void deleteBySymbolAndBarDateBefore(String symbol, LocalDate cutoffDate);
}
