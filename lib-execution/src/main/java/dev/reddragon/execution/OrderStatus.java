package dev.reddragon.execution;

public enum OrderStatus {
    WORKING(false),
    FILLED(true),
    CANCELLED(true),
    REJECTED(true);

    private final boolean terminal;

    OrderStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean terminal() {
        return terminal;
    }
}
