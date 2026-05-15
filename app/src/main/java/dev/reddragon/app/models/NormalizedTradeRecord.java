package dev.reddragon.app.models;

import java.time.Instant;

/**
 * Canonical trade row for the framework's historical analysis stage.
 */
public record NormalizedTradeRecord(
        Instant timestamp,
        String ticker,
        String side,
        double quantity,
        double price,
        Double realizedPnl,
        String account,
        String strategyType,
        String marketState
) {
}
