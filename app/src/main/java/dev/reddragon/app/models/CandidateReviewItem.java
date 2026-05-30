package dev.reddragon.app.models;

import java.time.Instant;
import java.util.List;

/**
 * One row on the candidate review surface: a de-duplicated verdict with enough
 * context for the trader to decide whether to look deeper.
 */
public record CandidateReviewItem(
        Long verdictId,
        String candidateId,
        String symbol,
        String verdict,
        String deploymentTier,
        double score,
        List<String> reasonCodes,
        List<String> explanations,
        /** Most recent regime label for this candidate (null if not available). */
        String regimeLabel,
        long noteCount,
        String overrideVerdict,
        Instant reviewedAt
) {
}
