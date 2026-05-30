package dev.reddragon.app.services.marketstate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.reddragon.analytics.services.MarketStateClassifier;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import dev.reddragon.persistence.domains.MarketStateSnapshotEntity;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.IntradayBarRepository;
import dev.reddragon.persistence.services.repositories.MarketStateSnapshotRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketStateSnapshotRunnerTest {

    @Test
    void runFetchesIntradayBarsAndPersistsMarketStateSnapshot() {
        MarketDataProvider marketDataProvider = mock(MarketDataProvider.class);
        IntradayBarRepository intradayBarRepository = mock(IntradayBarRepository.class);
        MarketStateSnapshotRepository snapshotRepository = mock(MarketStateSnapshotRepository.class);
        List<IntradayBar> bars = List.of(
                new IntradayBar("asts", Instant.parse("2026-05-28T13:30:00Z"), 10.0, 10.4, 9.9, 10.2, 1000, 10.1),
                new IntradayBar("asts", Instant.parse("2026-05-28T13:35:00Z"), 10.2, 10.8, 10.1, 10.7, 1400, 10.4),
                new IntradayBar("asts", Instant.parse("2026-05-28T13:40:00Z"), 10.7, 11.0, 10.6, 10.9, 1600, 10.6));
        when(marketDataProvider.intradayBars(eq("ASTS"), any(Instant.class), any(Instant.class), eq(Duration.ofMinutes(5))))
                .thenReturn(bars);
        when(snapshotRepository.save(any(MarketStateSnapshotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MarketStateSnapshotRunner runner = new MarketStateSnapshotRunner(
                marketDataProvider,
                new IntradayStructureSnapshotBuilder(),
                new MarketStateClassifier(),
                intradayBarRepository,
                snapshotRepository,
                new PersistenceMapper());

        MarketStateSnapshotEntity snapshot = runner.run("asts", 8, 5);

        assertEquals("ASTS", snapshot.getSymbol());
        assertEquals("SCHEDULED_INTRADAY", snapshot.getSource());
        assertEquals(3, snapshot.getBarCount());
        assertTrue(snapshot.getConfidence() >= 0.0);
        verify(intradayBarRepository).saveAll(any());
        verify(snapshotRepository).save(any(MarketStateSnapshotEntity.class));
    }
}
