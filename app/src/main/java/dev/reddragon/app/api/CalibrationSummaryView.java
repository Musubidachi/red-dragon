package dev.reddragon.app.api;

public record CalibrationSummaryView(
        int sampleSize,
        double winRate,
        double averageReturn,
        double averageDrawdown
) {
}
