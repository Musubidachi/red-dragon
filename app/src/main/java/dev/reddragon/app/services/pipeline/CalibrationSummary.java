package dev.reddragon.app.services.pipeline;

public record CalibrationSummary(int sampleSize, double winRate, double averageReturn, double averageDrawdown) {
}
