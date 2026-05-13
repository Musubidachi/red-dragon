package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.CalibrationOutcomeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CalibrationOutcomeRepository extends JpaRepository<CalibrationOutcomeEntity, Long> {
    List<CalibrationOutcomeEntity> findTop100ByOrderByObservedAtDesc();
    List<CalibrationOutcomeEntity> findTop100BySymbolOrderByObservedAtDesc(String symbol);
    long deleteBySymbol(String symbol);
    long countBySymbol(String symbol);
    CalibrationOutcomeEntity findTopByOrderByObservedAtDesc();

    @Query("select distinct c.symbol from CalibrationOutcomeEntity c order by c.symbol asc")
    List<String> findDistinctSymbols();

    List<CalibrationOutcomeEntity> findTop100ByOrderByRealizedReturnDesc();
    List<CalibrationOutcomeEntity> findTop100ByOrderByRealizedReturnAsc();
    List<CalibrationOutcomeEntity> findTop100ByOrderByMaxDrawdownDesc();
}
