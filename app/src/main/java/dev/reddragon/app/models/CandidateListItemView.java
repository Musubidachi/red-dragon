package dev.reddragon.app.models;

import java.time.Instant;

public record CandidateListItemView(
        String candidateId,
        String symbol,
        String companyName,
        String catalystType,
        String sourceType,
        String sourceId,
        String sourceUrl,
        Instant observedAt,
        String headline,
        String summary
) {
}
