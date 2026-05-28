package dev.reddragon.execution;

public enum OrderType {
    MARKET(false),
    LIMIT(true);

    private final boolean requiresLimitPrice;

    OrderType(boolean requiresLimitPrice) {
        this.requiresLimitPrice = requiresLimitPrice;
    }

    public boolean requiresLimitPrice() {
        return requiresLimitPrice;
    }
}
