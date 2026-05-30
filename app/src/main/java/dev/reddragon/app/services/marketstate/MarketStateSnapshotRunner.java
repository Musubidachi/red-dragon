package dev.reddragon.app.services.marketstate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.reddragon.analytics.services.MarketStateClassifier;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import dev.reddragon.domain.models.MarketStateSignal;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import dev.reddragon.persistence.domains.MarketStateSnapshotEntity;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.IntradayBarRepository;
import dev.reddragon.persistence.services.repositories.MarketStateSnapshotRepository;
import dev.reddragon.persistence.utilities.PersistenceStringUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketStateSnapshotRunner {

    private static final String SOURCE_SCHEDULED_INTRADAY = "SCHEDULED_INTRADAY";

    private final MarketDataProvider marketDataProvider;
    private final IntradayStructureSnapshotBuilder snapshotBuilder;
    private final MarketStateClassifier marketStateClassifier;
    private final IntradayBarRepository intradayBarRepository;
    private final MarketStateSnapshotRepository snapshotRepository;
    private final PersistenceMapper persistenceMapper;

    public MarketStateSnapshotEntity run(String symbol, int lookbackHours, long intervalMinutes) {
        String normalizedSymbol = symbol.trim().toUpperCase();
        Instant observedAt = Instant.now();
        Instant from = observedAt.minus(Duration.ofHours(Math.max(1, lookbackHours)));
        List<IntradayBar> bars = marketDataProvider.intradayBars(
                normalizedSymbol,
                from,
                observedAt,
                Duration.ofMinutes(Math.max(1, intervalMinutes)));
        if (bars == null || bars.isEmpty()) {
            throw new IllegalStateException("No intraday bars available for " + normalizedSymbol);
        }

        persistIntradayBars(bars);
        MarketIntradayStructureSnapshot structureSnapshot = snapshotBuilder.process(bars);
        MarketStateSignal signal = marketStateClassifier.process(structureSnapshot);
        return snapshotRepository.save(toEntity(
                observedAt,
                structureSnapshot,
                signal,
                bars.size()));
    }

    private void persistIntradayBars(List<IntradayBar> bars) {
        intradayBarRepository.saveAll(bars.stream()
                .filter(bar -> !intradayBarRepository.existsBySymbolAndStartTime(bar.symbol(), bar.startTime()))
                .map(persistenceMapper::toIntradayBarEntity)
                .toList());
    }

    private MarketStateSnapshotEntity toEntity(
            Instant observedAt,
            MarketIntradayStructureSnapshot structureSnapshot,
            MarketStateSignal signal,
            int barCount
    ) {
        return MarketStateSnapshotEntity.builder()
                .symbol(structureSnapshot.symbol())
                .observedAt(observedAt)
                .source(SOURCE_SCHEDULED_INTRADAY)
                .regimeLabel(signal.regimeLabel().name())
                .confidence(signal.confidence())
                .equilibriumRestorationProbability(signal.equilibriumRestorationProbability())
                .deploymentSupported(signal.deploymentSupported())
                .sessionVwap(structureSnapshot.sessionVwap())
                .latestClose(structureSnapshot.latestClose())
                .vwapDistancePercent(structureSnapshot.vwapDistancePercent())
                .vwapReclaimStrength(structureSnapshot.vwapReclaimStrength())
                .directionalPersistenceScore(structureSnapshot.directionalPersistenceScore())
                .rotationalQualityScore(structureSnapshot.rotationalQualityScore())
                .intradayTrendStrength(structureSnapshot.intradayTrendStrength())
                .aboveVwap(structureSnapshot.aboveVwap())
                .barCount(barCount)
                .notes(PersistenceStringUtils.joinText(signal.notes()))
                .build();
    }
}
