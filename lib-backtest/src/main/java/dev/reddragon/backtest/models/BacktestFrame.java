package dev.reddragon.backtest.models;

import dev.reddragon.ingestion.models.TradeCandidate;
import dev.reddragon.marketdata.models.MarketBar;

import java.util.List;

/**
 * Historical replay input for one candidate at one point in time.
 */
public record BacktestFrame(
        TradeCandidate candidate,
        List<MarketBar> bars
) {
    public BacktestFrame {
        bars = List.copyOf(bars == null ? List.of() : bars);
    }
}
