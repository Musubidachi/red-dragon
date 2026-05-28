package dev.reddragon.app.controllers;

import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketIntradayStructureSnapshot;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Thin orchestration endpoint for market-structure generation.
 */
@RestController
@RequestMapping("/api/market-structure")
@RequiredArgsConstructor
public class MarketStructureController {

    private final IntradayStructureSnapshotBuilder snapshotBuilder;

    @PostMapping("/intraday")
    public MarketIntradayStructureSnapshot process(
            @RequestBody List<IntradayBar> bars
    ) {
        return snapshotBuilder.process(bars);
    }
}
