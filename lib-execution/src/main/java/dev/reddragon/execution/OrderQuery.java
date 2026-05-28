package dev.reddragon.execution;

import java.time.Instant;

public record OrderQuery(
        String symbol,
        OrderStatus status,
        Instant createdFrom,
        Instant createdTo
) {
    public OrderQuery {
        symbol = symbol == null || symbol.isBlank() ? null : symbol.trim().toUpperCase();
    }

    public static OrderQuery all() {
        return new OrderQuery(null, null, null, null);
    }

    public boolean matches(Order order) {
        if (symbol != null && !symbol.equals(order.symbol())) {
            return false;
        }
        if (status != null && status != order.status()) {
            return false;
        }
        if (createdFrom != null && order.createdAt().isBefore(createdFrom)) {
            return false;
        }
        return createdTo == null || !order.createdAt().isAfter(createdTo);
    }
}
