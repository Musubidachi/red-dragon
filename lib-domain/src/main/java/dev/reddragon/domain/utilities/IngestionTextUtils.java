package dev.reddragon.domain.utilities;

import lombok.experimental.UtilityClass;

/**
 * Shared domain text normalization helpers.
 *
 * <p>The historical name comes from the first ingestion use case, but the class
 * is intentionally placed in {@code lib-domain} so model constructors can share
 * the same cleanup and symbol normalization rules across modules.
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
