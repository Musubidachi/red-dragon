package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.MarketBarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MarketBarRepository extends JpaRepository<MarketBarEntity, Long> {

    boolean existsBySymbolAndBarDate(String symbol, LocalDate barDate);

    List<MarketBarEntity> findBySymbolAndBarDateBetweenOrderByBarDateAsc(
            String symbol,
            LocalDate from,
            LocalDate to
    );

    List<MarketBarEntity> findTop60BySymbolOrderByBarDateDesc(String symbol);

    /** Deletes stale bar data older than the given cutoff date for a symbol. */
    void deleteBySymbolAndBarDateBefore(String symbol, LocalDate cutoffDate);
}
