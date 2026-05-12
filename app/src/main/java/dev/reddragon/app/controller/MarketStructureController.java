package dev.reddragon.app.controller;

import dev.reddragon.marketdata.model.IntradayBar;
import dev.reddragon.marketdata.model.MarketIntradayStructureSnapshot;
import dev.reddragon.marketdata.service.IntradayStructureSnapshotBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Thin orchestration endpoint for market-structure generation.
 */
@RestController
@RequestMapping("/market-structure")
public class MarketStructureController {

    private final IntradayStructureSnapshotBuilder snapshotBuilder;

    public MarketStructureController() {
        this.snapshotBuilder = new IntradayStructureSnapshotBuilder();
    }

    @PostMapping("/intraday")
    public MarketIntradayStructureSnapshot process(
            @RequestBody List<IntradayBar> bars
    ) {
        return snapshotBuilder.process(bars);
    }
}
