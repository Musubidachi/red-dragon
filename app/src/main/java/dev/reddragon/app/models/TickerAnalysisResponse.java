package dev.reddragon.app.models;

import java.util.List;

import dev.reddragon.domain.models.TradeCandidate;

public record TickerAnalysisResponse(
        String ticker,
        String cik,
        String analysisMode,
        SourceCoverage sourceCoverage,
        TradeCandidate selectedCandidate,
        PipelineRunResult pipelineResult,
        TickerResearchSummary research,
        List<TradeCandidate> secCandidates,
        List<String> notes
) {
}
