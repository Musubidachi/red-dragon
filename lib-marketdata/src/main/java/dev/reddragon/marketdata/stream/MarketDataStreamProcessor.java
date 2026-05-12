package dev.reddragon.marketdata.stream;

import java.util.List;
import java.util.Objects;

/**
 * Simple streaming abstraction for consuming market-data events.
 */
public class MarketDataStreamProcessor {

    /**
     * Main processing flow.
     */
    public List<MarketDataEvent> process(List<MarketDataEvent> events) {
        Objects.requireNonNull(events, "events are required");
        return List.copyOf(events);
    }
}
