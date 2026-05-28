package dev.reddragon.execution;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic broker implementation that models order lifecycle locally and
 * never calls a live broker.
 */
public class DryRunBrokerClient implements BrokerClient {

    private final AccountSummary accountSummary;
    private final List<Position> positions;
    private final Clock clock;
    private final Map<String, Order> ordersById = new LinkedHashMap<>();

    public DryRunBrokerClient(AccountSummary accountSummary, List<Position> positions, Clock clock) {
        this.accountSummary = Objects.requireNonNull(accountSummary, "accountSummary is required");
        this.positions = List.copyOf(positions == null ? List.of() : positions);
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public AccountSummary accountSummary() {
        return accountSummary;
    }

    @Override
    public List<Position> positions() {
        return positions;
    }

    @Override
    public synchronized List<Order> orders(OrderQuery query) {
        OrderQuery effectiveQuery = query == null ? OrderQuery.all() : query;
        return ordersById.values().stream()
                .filter(effectiveQuery::matches)
                .sorted(Comparator.comparing(Order::createdAt))
                .toList();
    }

    @Override
    public synchronized Order order(String orderId) {
        Order order = ordersById.get(requireText(orderId, "orderId"));
        if (order == null) {
            throw new IllegalArgumentException("Unknown orderId: " + orderId);
        }
        return order;
    }

    @Override
    public synchronized OrderResponse placeOrder(OrderRequest request) {
        Objects.requireNonNull(request, "request is required");
        String orderId = "DRY-" + request.clientOrderId();
        Order existing = ordersById.get(orderId);
        if (existing != null) {
            return new OrderResponse(orderId, existing.status(), "Duplicate dry-run order ignored");
        }
        Instant now = clock.instant();
        Order order = new Order(
                orderId,
                request.clientOrderId(),
                request.symbol(),
                request.assetType(),
                request.quantity(),
                request.description(),
                OrderStatus.WORKING,
                now,
                null,
                List.of()
        );
        ordersById.put(orderId, order);
        return new OrderResponse(orderId, OrderStatus.WORKING, "Dry-run order accepted");
    }

    @Override
    public synchronized OrderResponse cancelOrder(String orderId) {
        Order current = order(orderId);
        if (current.status().terminal()) {
            return new OrderResponse(current.orderId(), current.status(), "Terminal order cannot be cancelled");
        }
        Order cancelled = current.withStatus(OrderStatus.CANCELLED, clock.instant());
        ordersById.put(cancelled.orderId(), cancelled);
        return new OrderResponse(cancelled.orderId(), cancelled.status(), "Dry-run order cancelled");
    }

    @Override
    public synchronized OrderResponse recordFill(String orderId, Fill fill) {
        Order current = order(orderId);
        Objects.requireNonNull(fill, "fill is required");
        if (current.status().terminal()) {
            return new OrderResponse(current.orderId(), current.status(), "Terminal order cannot be filled");
        }
        List<Fill> fills = new ArrayList<>(current.fills());
        fills.add(fill);
        Order filled = current.withFills(OrderStatus.FILLED, clock.instant(), fills);
        ordersById.put(filled.orderId(), filled);
        return new OrderResponse(filled.orderId(), filled.status(), "Dry-run fill recorded");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
