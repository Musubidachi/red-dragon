package dev.reddragon.app.models;

import java.util.List;

public record SourceCoverage(
        SourceCoverageStatus marketData,
        SourceCoverageStatus secFilings,
        SourceCoverageStatus llmResearch,
        double overallConfidence,
        List<String> missingSources,
        List<String> degradedSources
) {
    public SourceCoverage {
        overallConfidence = Math.max(0.0, Math.min(1.0, overallConfidence));
        missingSources = List.copyOf(missingSources == null ? List.of() : missingSources);
        degradedSources = List.copyOf(degradedSources == null ? List.of() : degradedSources);
    }
}
