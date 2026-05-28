package dev.reddragon.execution;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DryRunBrokerClientTest {

    private static final Instant NOW = Instant.parse("2026-05-27T12:00:00Z");

    @Test
    void exposesAccountAndPositionsWithoutLiveBrokerCalls() {
        DryRunBrokerClient client = client();

        assertEquals("dry-account", client.accountSummary().accountId());
        assertEquals(1, client.positions().size());
        assertEquals("NVDA", client.positions().get(0).symbol());
    }

    @Test
    void placeOrderCreatesWorkingOrderWithDeterministicDryRunId() {
        DryRunBrokerClient client = client();

        OrderResponse response = client.placeOrder(equityOrder("order-1"));
        Order order = client.order(response.orderId());

        assertEquals("DRY-order-1", response.orderId());
        assertEquals(OrderStatus.WORKING, response.status());
        assertEquals("NVDA", order.symbol());
        assertEquals(AssetType.EQUITY, order.assetType());
        assertEquals(NOW, order.createdAt());
    }

    @Test
    void duplicateClientOrderIdDoesNotCreateSecondOrder() {
        DryRunBrokerClient client = client();

        String orderId = client.placeOrder(equityOrder("order-1")).orderId();
        client.recordFill(orderId, new Fill("fill-1", BigDecimal.TEN, new BigDecimal("100.50"), NOW));
        OrderResponse duplicate = client.placeOrder(equityOrder("order-1"));

        assertEquals(OrderStatus.FILLED, duplicate.status());
        assertEquals(1, client.orders(OrderQuery.all()).size());
    }

    @Test
    void recordsFillAndRejectsTerminalCancellation() {
        DryRunBrokerClient client = client();
        String orderId = client.placeOrder(equityOrder("order-2")).orderId();

        OrderResponse fillResponse = client.recordFill(orderId, new Fill(
                "fill-1",
                BigDecimal.TEN,
                new BigDecimal("100.50"),
                NOW.plusSeconds(5)
        ));
        OrderResponse cancelResponse = client.cancelOrder(orderId);

        assertEquals(OrderStatus.FILLED, fillResponse.status());
        assertEquals(OrderStatus.FILLED, cancelResponse.status());
        assertEquals(1, client.order(orderId).fills().size());
    }

    @Test
    void cancelMovesWorkingOrderToTerminalCancelled() {
        DryRunBrokerClient client = client();
        String orderId = client.placeOrder(equityOrder("order-3")).orderId();

        OrderResponse response = client.cancelOrder(orderId);

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(OrderStatus.CANCELLED, client.order(orderId).status());
    }

    @Test
    void orderQueryFiltersBySymbolAndStatus() {
        DryRunBrokerClient client = client();
        String first = client.placeOrder(equityOrder("order-4")).orderId();
        client.recordFill(first, new Fill("fill-4", BigDecimal.TEN, new BigDecimal("100.50"), NOW));
        client.placeOrder(new EquityOrderRequest(
                "MSFT",
                OrderSide.BUY,
                BigDecimal.ONE,
                OrderType.MARKET,
                null,
                OrderDuration.DAY,
                "order-5"
        ));

        List<Order> filledNvda = client.orders(new OrderQuery("nvda", OrderStatus.FILLED, null, null));

        assertEquals(1, filledNvda.size());
        assertEquals("DRY-order-4", filledNvda.get(0).orderId());
    }

    @Test
    void optionOrdersAreSupportedInDryRunLifecycle() {
        DryRunBrokerClient client = client();
        SingleLegOptionOrderRequest request = new SingleLegOptionOrderRequest(
                "NVDA",
                "NVDA  260117C00150000",
                OptionOrderSide.SELL_TO_OPEN,
                BigDecimal.ONE,
                OrderType.LIMIT,
                new BigDecimal("4.20"),
                OrderDuration.DAY,
                "option-1"
        );

        OrderResponse response = client.placeOrder(request);

        assertEquals(OrderStatus.WORKING, response.status());
        assertEquals(AssetType.OPTION, client.order(response.orderId()).assetType());
    }

    @Test
    void liveClientFailsClosed() {
        LiveBrokerClient live = new LiveBrokerClient();

        assertThrows(UnsupportedOperationException.class, live::accountSummary);
    }

    private DryRunBrokerClient client() {
        return new DryRunBrokerClient(
                new AccountSummary(
                        "dry-account",
                        new BigDecimal("1000.00"),
                        new BigDecimal("1500.00"),
                        new BigDecimal("2500.00"),
                        false
                ),
                List.of(new Position(
                        "NVDA",
                        AssetType.EQUITY,
                        BigDecimal.TEN,
                        new BigDecimal("90.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("1000.00"),
                        null
                )),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private EquityOrderRequest equityOrder(String clientOrderId) {
        return new EquityOrderRequest(
                "nvda",
                OrderSide.BUY,
                BigDecimal.TEN,
                OrderType.LIMIT,
                new BigDecimal("100.50"),
                OrderDuration.DAY,
                clientOrderId
        );
    }
}
