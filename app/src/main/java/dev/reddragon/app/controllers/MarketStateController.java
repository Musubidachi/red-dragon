package dev.reddragon.app.controllers;

import dev.reddragon.analytics.models.MarketStateSignal;
import dev.reddragon.analytics.services.MarketStateClassifier;
import dev.reddragon.marketdata.models.IntradayBar;
import dev.reddragon.marketdata.models.MarketIntradayStructureSnapshot;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Produces regime-aware market-state signals from intraday bars.
 */
@RestController
@RequestMapping("/api/market-state")
public class MarketStateController {

    private final IntradayStructureSnapshotBuilder snapshotBuilder;
    private final MarketStateClassifier marketStateClassifier;

    public MarketStateController() {
        this.snapshotBuilder = new IntradayStructureSnapshotBuilder();
        this.marketStateClassifier = new MarketStateClassifier();
    }

    @PostMapping("/classify")
    public MarketStateSignal classify(@RequestBody List<IntradayBar> bars) {
        MarketIntradayStructureSnapshot snapshot = snapshotBuilder.process(bars);
        return marketStateClassifier.process(snapshot);
    }
}
