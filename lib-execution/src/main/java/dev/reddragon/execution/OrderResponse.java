package dev.reddragon.execution;

public record OrderResponse(
        String orderId,
        OrderStatus status,
        String message
) {
    public OrderResponse {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        message = message == null ? "" : message;
    }
}
