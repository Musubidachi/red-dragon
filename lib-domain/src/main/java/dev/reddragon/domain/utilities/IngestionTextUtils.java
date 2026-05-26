package dev.reddragon.domain.utilities;

import lombok.experimental.UtilityClass;

/**
 * Shared text normalization helpers for ingestion adapters.
 */
@UtilityClass
public class IngestionTextUtils {

    public String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public String requireText(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    public String normalizeSymbol(String symbol) {
        return requireText(symbol, "symbol").toUpperCase();
    }
}
