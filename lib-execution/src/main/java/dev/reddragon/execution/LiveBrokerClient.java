package dev.reddragon.execution;

import java.util.List;

/**
 * Guard implementation for live mode until a provider-specific broker client
 * has been audited and wired by the application layer.
 */
public class LiveBrokerClient implements BrokerClient {

    @Override
    public AccountSummary accountSummary() {
        throw unsupported();
    }

    @Override
    public List<Position> positions() {
        throw unsupported();
    }

    @Override
    public List<Order> orders(OrderQuery query) {
        throw unsupported();
    }

    @Override
    public Order order(String orderId) {
        throw unsupported();
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        throw unsupported();
    }

    @Override
    public OrderResponse cancelOrder(String orderId) {
        throw unsupported();
    }

    @Override
    public OrderResponse recordFill(String orderId, Fill fill) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(
                "Live broker execution is not implemented; use DryRunBrokerClient");
    }
}
