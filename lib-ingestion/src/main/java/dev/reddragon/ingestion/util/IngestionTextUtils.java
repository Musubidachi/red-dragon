package dev.reddragon.ingestion.util;

import lombok.experimental.UtilityClass;

/**
 * Shared text normalization helpers for ingestion adapters.
 */
@UtilityClass
public class IngestionTextUtils {

    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static String requireText(String value, String fieldName) {
        String cleaned = clean(value);
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return cleaned;
    }

    public static String normalizeSymbol(String symbol) {
        return requireText(symbol, "symbol").toUpperCase();
    }
}
