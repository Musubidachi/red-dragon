package dev.reddragon.app.models;

public record CalibrationSummaryDetailView(
        int sampleSize,
        double winRate,
        double averageReturn,
        double averageDrawdown,
        double minReturn,
        double maxReturn,
        double medianReturn,
        double minDrawdown,
        double maxDrawdown,
        double medianDrawdown,
        double averageHoldingDays
) {
}
