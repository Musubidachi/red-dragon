package dev.reddragon.app.controllers;

import dev.reddragon.analytics.models.RegimeLabel;
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
 * (across all symbols, newest-first, up to 50 entries).
 */
@RestController
@RequestMapping("/api/regime/history")
@RequiredArgsConstructor
public class RegimeHistoryController {

    private static final int DEFAULT_LOOKBACK_HOURS = 48;

    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @GetMapping
    public List<AnalyticsSnapshotEntity> history(
            @RequestParam(required = false)                              String regime,
            @RequestParam(defaultValue = "" + DEFAULT_LOOKBACK_HOURS)   int lookbackHours
    ) {
        Instant since = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);

        if (regime != null && !regime.isBlank()) {
            // Validate the regime label
            RegimeLabel.valueOf(regime.trim().toUpperCase()); // throws if invalid
            return analyticsSnapshotRepository
                    .findByRegimeLabelAndObservedAtAfterOrderByObservedAtDesc(
                            regime.trim().toUpperCase(), since);
        }

        // No filter — return all snapshots in window, newest-first, capped at 50
        return analyticsSnapshotRepository.findAll().stream()
                .filter(s -> s.getObservedAt().isAfter(since))
                .sorted((a, b) -> b.getObservedAt().compareTo(a.getObservedAt()))
                .limit(50)
                .toList();
    }
}
