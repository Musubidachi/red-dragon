package dev.reddragon.execution;

import java.math.BigDecimal;
import java.util.Objects;

public record AccountSummary(
        String accountId,
        BigDecimal cashBalance,
        BigDecimal buyingPower,
        BigDecimal totalEquity,
        boolean dayTraderFlag
) {
    public AccountSummary {
        accountId = requireText(accountId, "accountId");
        cashBalance = requireNonNegative(cashBalance, "cashBalance");
        buyingPower = requireNonNegative(buyingPower, "buyingPower");
        totalEquity = requireNonNegative(totalEquity, "totalEquity");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " is required");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }
}
