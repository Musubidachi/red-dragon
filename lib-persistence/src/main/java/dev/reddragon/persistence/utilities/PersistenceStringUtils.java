package dev.reddragon.persistence.utilities;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Shared persistence formatting helpers.
 */
@UtilityClass
public class PersistenceStringUtils {

    public String joinNames(Collection<? extends Enum<?>> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    public String joinText(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" | "));
    }
}
