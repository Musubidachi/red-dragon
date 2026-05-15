package dev.reddragon.app.models;

public record CalibrationSummaryView(
        int sampleSize,
        double winRate,
        double averageReturn,
        double averageDrawdown
) {
}
