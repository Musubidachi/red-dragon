package dev.reddragon.execution;

import java.util.List;

public interface BrokerClient {

    AccountSummary accountSummary();

    List<Position> positions();

    List<Order> orders(OrderQuery query);

    Order order(String orderId);

    OrderResponse placeOrder(OrderRequest request);

    OrderResponse cancelOrder(String orderId);

    OrderResponse recordFill(String orderId, Fill fill);
}
