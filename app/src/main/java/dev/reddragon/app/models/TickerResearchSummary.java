package dev.reddragon.app.models;

import java.util.List;

import dev.reddragon.domain.models.CandidateCatalystType;

public record TickerResearchSummary(
        boolean llmEnabled,
        boolean catalystFound,
        CandidateCatalystType catalystType,
        String headline,
        String summary,
        double structuralRealityScore,
        double materialSignificanceScore,
        double earlynessScore,
        double reflexivityPotentialScore,
        List<String> riskFlags,
        List<String> sources,
        String rawNarrative
) {
    public static TickerResearchSummary unavailable(String reason) {
        return new TickerResearchSummary(
                false,
                false,
                CandidateCatalystType.MANUAL_THESIS,
                "No LLM research available",
                reason,
                0.45,
                0.40,
                0.50,
                0.35,
                List.of("LLM research unavailable"),
                List.of(),
                reason
        );
    }
}
