package dev.reddragon.execution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record Fill(
        String fillId,
        BigDecimal quantity,
        BigDecimal price,
        Instant filledAt
) {
    public Fill {
        fillId = requireText(fillId, "fillId");
        quantity = requirePositive(quantity, "quantity");
        price = requirePositive(price, "price");
        filledAt = Objects.requireNonNull(filledAt, "filledAt is required");
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
