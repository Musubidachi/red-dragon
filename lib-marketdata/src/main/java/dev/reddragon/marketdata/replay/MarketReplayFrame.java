package dev.reddragon.marketdata.replay;

import dev.reddragon.marketdata.model.IntradayBar;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

/**
 * Represents a replayable historical market frame.
 */
@Value
@Accessors(fluent = true)
public class MarketReplayFrame {
    String symbol;
    Instant timestamp;
    List<IntradayBar> bars;
}
