package dev.reddragon.execution;

import java.math.BigDecimal;
import java.util.Objects;

public record Position(
        String symbol,
        AssetType assetType,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        String optionSymbol
) {
    public Position {
        symbol = requireText(symbol, "symbol").toUpperCase();
        assetType = Objects.requireNonNull(assetType, "assetType is required");
        quantity = Objects.requireNonNull(quantity, "quantity is required");
        averageCost = requireNonNegative(averageCost, "averageCost");
        currentPrice = requireNonNegative(currentPrice, "currentPrice");
        marketValue = Objects.requireNonNull(marketValue, "marketValue is required");
        optionSymbol = optionSymbol == null || optionSymbol.isBlank() ? null : optionSymbol.trim();
        if (assetType == AssetType.OPTION && optionSymbol == null) {
            throw new IllegalArgumentException("optionSymbol is required for option positions");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }
}
