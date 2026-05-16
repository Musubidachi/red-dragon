package dev.reddragon.persistence.services.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.IntradayBarEntity;

public interface IntradayBarRepository extends JpaRepository<IntradayBarEntity, Long> {

    boolean existsBySymbolAndStartTime(String symbol, Instant startTime);

    List<IntradayBarEntity> findBySymbolAndStartTimeBetweenOrderByStartTimeAsc(
            String symbol,
            Instant from,
            Instant to
    );
}
