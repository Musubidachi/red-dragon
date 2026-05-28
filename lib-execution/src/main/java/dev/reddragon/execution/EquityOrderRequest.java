package dev.reddragon.execution;

import java.math.BigDecimal;
import java.util.Objects;

public record EquityOrderRequest(
        String symbol,
        OrderSide side,
        BigDecimal quantity,
        OrderType orderType,
        BigDecimal limitPrice,
        OrderDuration duration,
        String clientOrderId
) implements OrderRequest {
    public EquityOrderRequest {
        symbol = requireText(symbol, "symbol").toUpperCase();
        side = Objects.requireNonNull(side, "side is required");
        quantity = requirePositive(quantity, "quantity");
        orderType = Objects.requireNonNull(orderType, "orderType is required");
        duration = Objects.requireNonNull(duration, "duration is required");
        clientOrderId = requireText(clientOrderId, "clientOrderId");
        if (orderType.requiresLimitPrice()) {
            limitPrice = requirePositive(limitPrice, "limitPrice");
        } else if (limitPrice != null && limitPrice.signum() <= 0) {
            throw new IllegalArgumentException("limitPrice must be positive when supplied");
        }
    }

    @Override
    public AssetType assetType() {
        return AssetType.EQUITY;
    }

    @Override
    public String description() {
        return side + " " + quantity + " " + symbol + " " + orderType;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
