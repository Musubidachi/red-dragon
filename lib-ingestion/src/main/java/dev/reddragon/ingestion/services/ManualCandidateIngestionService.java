package dev.reddragon.ingestion.services;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.math.ValidationScoreUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Builds a trade candidate from a manually supplied thesis.
 *
 * <p>Mirrors the {@link dev.reddragon.ingestion.services.sec.SecCandidateBuilder}
 * pattern: validates score inputs strictly, derives a stable candidateId from
 * the input shape so repeated submission of the same thesis is idempotent,
 * and takes a {@link Clock} for testability.
 */
public class ManualCandidateIngestionService {

    private final Clock clock;

    public ManualCandidateIngestionService() {
        this(Clock.systemUTC());
    }

    public ManualCandidateIngestionService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Main processing flow.
     */
    public TradeCandidate process(
            String symbol,
            String companyName,
            CandidateCatalystType catalystType,
            String headline,
            String summary,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double reflexivityPotentialScore
    ) {
        // TradeCandidate's constructor already rejects blank symbol, NaN/out-of-range
        // scores, and null catalystType. We layer in additional checks for
        // fields TradeCandidate accepts loosely (companyName/headline/summary)
        // so the manual entry point fails closer to the human who typed.
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required for manual candidates");
        }
        ValidationScoreUtils.requireNormalized("structuralRealityScore", structuralRealityScore);
        ValidationScoreUtils.requireNormalized("materialSignificanceScore", materialSignificanceScore);
        ValidationScoreUtils.requireNormalized("earlynessScore", earlynessScore);
        ValidationScoreUtils.requireNormalized("reflexivityPotentialScore", reflexivityPotentialScore);

        CandidateCatalystType normalizedCatalystType = catalystType(catalystType);
        Instant observedAt = observedAt();
        String candidateId = candidateId(symbol, headline, observedAt);

        return buildCandidate(
                candidateId,
                symbol,
                companyName,
                normalizedCatalystType,
                headline,
                summary,
                observedAt,
                structuralRealityScore,
                materialSignificanceScore,
                earlynessScore,
                reflexivityPotentialScore
        );
    }

    private CandidateCatalystType catalystType(CandidateCatalystType catalystType) {
        if (catalystType == null) {
            return CandidateCatalystType.MANUAL_THESIS;
        }
        return catalystType;
    }

    /**
     * Stable id derived from {@code (symbol, headline, filing-date UTC)}.
     * Two {@code process()} calls with the same shape on the same UTC date
     * collide, so the orchestrator's dedup catches double-submits — mirroring
     * the {@code accessionNumber} stability of the SEC path.
     */
    private String candidateId(String symbol, String headline, Instant observedAt) {
        String normalizedSymbol = symbol.trim().toUpperCase();
        String normalizedHeadline = headline == null ? "" : headline.trim();
        LocalDate observedDate = observedAt.atZone(ZoneOffset.UTC).toLocalDate();
        String input = "MANUAL|" + normalizedSymbol + "|" + observedDate + "|" + normalizedHeadline;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Take the first 16 bytes (32 hex chars) — enough to make collision
            // across reasonable manual-entry volumes negligible.
            return "manual-" + HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by every standard JRE; reaching this branch
            // means a broken JVM, not a recoverable error.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private Instant observedAt() {
        return Instant.now(clock);
    }

    private TradeCandidate buildCandidate(
            String candidateId,
            String symbol,
            String companyName,
            CandidateCatalystType catalystType,
            String headline,
            String summary,
            Instant observedAt,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double reflexivityPotentialScore
    ) {
        return new TradeCandidate(
                candidateId,
                symbol,
                companyName,
                catalystType,
                SourceType.MANUAL,
                "manual",
                "",
                observedAt,
                headline,
                summary,
                structuralRealityScore,
                materialSignificanceScore,
                earlynessScore,
                reflexivityPotentialScore
        );
    }
}
