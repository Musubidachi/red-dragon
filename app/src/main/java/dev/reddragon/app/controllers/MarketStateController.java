package dev.reddragon.app.controllers;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.analytics.services.MarketStateClassifier;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import dev.reddragon.domain.models.MarketStateSignal;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import dev.reddragon.persistence.domains.MarketStateSnapshotEntity;
import dev.reddragon.persistence.services.repositories.MarketStateSnapshotRepository;
import lombok.RequiredArgsConstructor;

/**
 * Produces regime-aware market-state signals from intraday bars.
 */
@RestController
@RequestMapping("/api/market-state")
@RequiredArgsConstructor
public class MarketStateController {

    private final IntradayStructureSnapshotBuilder snapshotBuilder;
    private final MarketStateClassifier marketStateClassifier;
    private final MarketStateSnapshotRepository snapshotRepository;

    @PostMapping("/classify")
    public MarketStateSignal classify(@RequestBody List<IntradayBar> bars) {
        MarketIntradayStructureSnapshot snapshot = snapshotBuilder.process(bars);
        return marketStateClassifier.process(snapshot);
    }

    @GetMapping("/snapshots")
    public List<MarketStateSnapshotEntity> snapshots(
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "25") int limit
    ) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        if (symbol == null || symbol.isBlank()) {
            return snapshotRepository.findAllByOrderByObservedAtDesc(PageRequest.of(0, safeLimit));
        }
        return snapshotRepository.findBySymbolOrderByObservedAtDesc(
                symbol.trim().toUpperCase(),
                PageRequest.of(0, safeLimit));
    }
}
