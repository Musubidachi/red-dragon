package dev.reddragon.marketdata.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provides simple rolling-window slices for market-data calculations.
 */
public class RollingWindowService {

    /**
     * Main processing flow.
     */
    public <T> List<List<T>> process(List<T> values, int windowSize) {
        Objects.requireNonNull(values, "values are required");

        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }

        if (values.size() < windowSize) {
            return List.of();
        }

        List<List<T>> windows = new ArrayList<>();

        for (int start = 0; start <= values.size() - windowSize; start++) {
            windows.add(List.copyOf(values.subList(start, start + windowSize)));
        }

        return List.copyOf(windows);
    }

    public <T> List<T> latest(List<T> values, int windowSize) {
        Objects.requireNonNull(values, "values are required");

        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }

        if (values.size() <= windowSize) {
            return List.copyOf(values);
        }

        return List.copyOf(values.subList(values.size() - windowSize, values.size()));
    }
}
