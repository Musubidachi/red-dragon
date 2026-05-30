package dev.reddragon.app.controllers;

import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Returns recent analytics snapshots grouped by regime label.
 *
 * <pre>
 * GET /api/regime/history?regime=SUPPORTIVE_TREND&lookbackHours=48
 * </pre>
 *
 * When no regime filter is supplied, snapshots for all regimes are returned
 * (across all symbols, newest-first, up to 50 entries). Use {@code symbol}
 * to scope the history to a single ticker.
 */
@RestController
@RequestMapping("/api/regime/history")
@RequiredArgsConstructor
public class RegimeHistoryController {

    private static final int DEFAULT_LOOKBACK_HOURS = 48;

    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @GetMapping
    public List<AnalyticsSnapshotEntity> history(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String regime,
            @RequestParam(defaultValue = "" + DEFAULT_LOOKBACK_HOURS) int lookbackHours
    ) {
        Instant since = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);
        String normalizedSymbol = symbol == null || symbol.isBlank()
                ? null
                : symbol.trim().toUpperCase();

        if (regime != null && !regime.isBlank()) {
            String normalizedRegime = regime.trim().toUpperCase();
            RegimeLabel.valueOf(normalizedRegime);
            if (normalizedSymbol != null) {
                return analyticsSnapshotRepository
                        .findBySymbolAndRegimeLabelAndObservedAtAfterOrderByObservedAtDesc(
                                normalizedSymbol,
                                normalizedRegime,
                                since);
            }
            return analyticsSnapshotRepository.findByRegimeLabelAndObservedAtAfterOrderByObservedAtDesc(
                    normalizedRegime,
                    since);
        }

        if (normalizedSymbol != null) {
            return analyticsSnapshotRepository.findBySymbolAndObservedAtAfterOrderByObservedAtDesc(
                    normalizedSymbol,
                    since);
        }

        return analyticsSnapshotRepository.findTop50ByObservedAtAfterOrderByObservedAtDesc(since);
    }
}
