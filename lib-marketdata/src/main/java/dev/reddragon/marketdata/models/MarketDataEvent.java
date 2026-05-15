package dev.reddragon.marketdata.models;

import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * Lightweight streaming event abstraction for market updates.
 */
@Value
@Accessors(fluent = true)
public class MarketDataEvent {
    String symbol;
    Instant timestamp;
    String eventType;
    String payload;
}
