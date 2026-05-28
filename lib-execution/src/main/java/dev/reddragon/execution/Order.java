package dev.reddragon.execution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Order(
        String orderId,
        String clientOrderId,
        String symbol,
        AssetType assetType,
        BigDecimal quantity,
        String description,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<Fill> fills
) {
    public Order {
        orderId = requireText(orderId, "orderId");
        clientOrderId = requireText(clientOrderId, "clientOrderId");
        symbol = requireText(symbol, "symbol").toUpperCase();
        assetType = Objects.requireNonNull(assetType, "assetType is required");
        quantity = requirePositive(quantity, "quantity");
        description = requireText(description, "description");
        status = Objects.requireNonNull(status, "status is required");
        createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        fills = List.copyOf(fills == null ? List.of() : fills);
    }

    public Order withStatus(OrderStatus nextStatus, Instant changedAt) {
        return new Order(
                orderId,
                clientOrderId,
                symbol,
                assetType,
                quantity,
                description,
                nextStatus,
                createdAt,
                changedAt,
                fills
        );
    }

    public Order withFills(OrderStatus nextStatus, Instant changedAt, List<Fill> nextFills) {
        return new Order(
                orderId,
                clientOrderId,
                symbol,
                assetType,
                quantity,
                description,
                nextStatus,
                createdAt,
                changedAt,
                nextFills
        );
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
