package dev.reddragon.persistence.services;

import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.IntradayBarEntity;
import dev.reddragon.persistence.domains.MarketBarEntity;
import dev.reddragon.persistence.domains.MarketQuoteObservationEntity;
import dev.reddragon.persistence.domains.MarketSnapshotEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;
import dev.reddragon.persistence.utilities.PersistenceStringUtils;
import dev.reddragon.domain.models.ReasonCode;
import dev.reddragon.domain.models.ValidationResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Maps pipeline domain objects into persistence entities.
 */
public class PersistenceMapper {

    public CandidateEntity toCandidateEntity(TradeCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate is required");

        // Builder construction (lib-persistence REVIEW.md Finding #8) —
        // named arguments make positional drift impossible. Audit columns
        // (createdAt / updatedAt / version) are JPA-managed and not set
        // here.
        return CandidateEntity.builder()
                .candidateId(candidate.candidateId())
                .symbol(candidate.symbol())
                .companyName(candidate.companyName())
                .catalystType(candidate.catalystType().name())
                .sourceType(candidate.sourceType().name())
                .sourceId(candidate.sourceId())
                .sourceUrl(candidate.sourceUrl())
                .observedAt(candidate.observedAt())
                .headline(candidate.headline())
                .summary(candidate.summary())
                .build();
    }

    /**
     * Map a {@link ValidationResult} into the verdict entity plus its
     * normalized reason rows. As of V12 the reasons live as child rows in
     * {@code validation_verdict_reason}; the legacy {@code reason_codes} and
     * {@code explanations} string columns are no longer written (they remain
     * on the table for historical rows).
     *
     * <p>The returned entity has its {@code reasons} collection populated
     * with each child's {@code verdict} back-reference pointing at the parent,
     * so a single {@code repository.save(entity)} cascade persists both rows.
     */
    public ValidationVerdictEntity toValidationVerdictEntity(ValidationResult result) {
        Objects.requireNonNull(result, "validation result is required");

        ValidationVerdictEntity entity = ValidationVerdictEntity.builder()
                .candidateId(result.candidateId())
                .symbol(result.symbol())
                .verdict(result.verdict().name())
                .deploymentTier(result.deploymentTier().name())
                .score(result.score())
                .idempotencyKey(validationVerdictIdempotencyKey(result))
                .reasonCodes(null)   // legacy column — no longer written (V12+)
                .explanations(null)  // legacy column — no longer written (V12+)
                .createdAt(Instant.now())
                .reasons(new ArrayList<>())
                .build();
        entity.getReasons().addAll(buildReasonEntities(entity, result));
        return entity;
    }

    private String validationVerdictIdempotencyKey(ValidationResult result) {
        StringBuilder payload = new StringBuilder();
        appendCanonical(payload, "v1");
        appendCanonical(payload, result.candidateId());
        appendCanonical(payload, result.symbol());
        appendCanonical(payload, result.verdict().name());
        appendCanonical(payload, result.deploymentTier().name());
        appendCanonical(payload, Double.toString(result.score()));

        appendCanonical(payload, Integer.toString(result.reasonCodes().size()));
        result.reasonCodes().forEach(code -> appendCanonical(payload, code.name()));

        appendCanonical(payload, Integer.toString(result.explanations().size()));
        result.explanations().forEach(explanation -> appendCanonical(payload, explanation));

        appendCanonical(payload, Integer.toString(result.factors().size()));
        result.factors().forEach(factor -> {
            appendCanonical(payload, factor.stage().name());
            appendCanonical(payload, Double.toString(factor.score()));
            appendCanonical(payload, Double.toString(factor.weight()));
            appendCanonical(payload, factor.reasonCode().name());
            appendCanonical(payload, factor.explanation());
        });

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            return "vv:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private void appendCanonical(StringBuilder payload, String value) {
        String normalized = value == null ? "" : value;
        payload.append(normalized.length())
                .append(':')
                .append(normalized);
    }

    /**
     * Build the {@link ValidationVerdictReasonEntity} list for a verdict.
     * Reasons and explanations are aligned by index (reasonCodes[i] pairs
     * with explanations[i]); when one list is longer than the other the
     * unmatched entries are emitted with the missing side as {@code null} or
     * with the reason-code defaulted to the legacy unknown sentinel.
     */
    private List<ValidationVerdictReasonEntity> buildReasonEntities(
            ValidationVerdictEntity parent,
            ValidationResult result
    ) {
        List<ReasonCode> codes = result.reasonCodes() == null ? List.of() : result.reasonCodes();
        List<String> explanations = result.explanations() == null ? List.of() : result.explanations();
        int rows = Math.max(codes.size(), explanations.size());
        List<ValidationVerdictReasonEntity> children = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            String code = i < codes.size() ? codes.get(i).name() : "UNSPECIFIED";
            String explanation = i < explanations.size() ? explanations.get(i) : null;
            ValidationVerdictReasonEntity child = ValidationVerdictReasonEntity.builder()
                    .verdict(parent)
                    .reasonCode(code)
                    .explanation(explanation)
                    .sortOrder((short) i)
                    .build();
            children.add(child);
        }
        return children;
    }

    public MarketBarEntity toMarketBarEntity(MarketBar bar) {
        Objects.requireNonNull(bar, "bar is required");

        return MarketBarEntity.builder()
                .symbol(bar.symbol())
                .barDate(bar.date())
                .openPrice(bar.open())
                .highPrice(bar.high())
                .lowPrice(bar.low())
                .closePrice(bar.close())
                .volume(bar.volume())
                .build();
    }

    public List<MarketBarEntity> toMarketBarEntities(List<MarketBar> bars) {
        if (bars == null || bars.isEmpty()) {
            return List.of();
        }
        return bars.stream()
                .map(this::toMarketBarEntity)
                .toList();
    }

    public IntradayBarEntity toIntradayBarEntity(IntradayBar bar) {
        Objects.requireNonNull(bar, "intraday bar is required");

        return IntradayBarEntity.builder()
                .symbol(bar.symbol())
                .startTime(bar.startTime())
                .openPrice(bar.open())
                .highPrice(bar.high())
                .lowPrice(bar.low())
                .closePrice(bar.close())
                .volume(bar.volume())
                .vwap(bar.vwap())
                .createdAt(Instant.now())
                .build();
    }

    public List<IntradayBarEntity> toIntradayBarEntities(List<IntradayBar> bars) {
        if (bars == null || bars.isEmpty()) {
            return List.of();
        }
        return bars.stream()
                .map(this::toIntradayBarEntity)
                .toList();
    }

    public MarketQuoteObservationEntity toMarketQuoteObservationEntity(MarketQuote quote) {
        Objects.requireNonNull(quote, "market quote is required");

        return MarketQuoteObservationEntity.builder()
                .symbol(quote.symbol())
                .observedAt(quote.observedAt())
                .lastPrice(quote.lastPrice())
                .bidPrice(quote.bidPrice())
                .askPrice(quote.askPrice())
                .volume(quote.volume())
                .quality(quote.quality().name())
                .notes(PersistenceStringUtils.joinText(quote.notes()))
                .build();
    }

    public MarketSnapshotEntity toMarketSnapshotEntity(String candidateId, MarketDataSnapshot snapshot) {
        Objects.requireNonNull(candidateId, "candidateId is required");
        Objects.requireNonNull(snapshot, "snapshot is required");

        return MarketSnapshotEntity.builder()
                .candidateId(candidateId)
                .symbol(snapshot.symbol())
                .observedAt(snapshot.observedAt())
                .latestClose(snapshot.latestClose())
                .previousClose(snapshot.previousClose())
                .gapPercent(snapshot.gapPercent())
                .averageTrueRange(snapshot.averageTrueRange())
                .rangePosition(snapshot.rangePosition())
                .averageVolume(snapshot.averageVolume())
                .liquidityScore(snapshot.liquidityScore())
                .volatilityStabilityScore(snapshot.volatilityStabilityScore())
                .quality(snapshot.quality().name())
                .notes(PersistenceStringUtils.joinText(snapshot.notes()))
                .relativeVolume(snapshot.relativeVolume())
                .vwapDeviation(snapshot.vwapDeviation())
                .directionalPersistence(snapshot.directionalPersistence())
                .build();
    }

    public AnalyticsSnapshotEntity toAnalyticsSnapshotEntity(AnalyticsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "analytics snapshot is required");

        return AnalyticsSnapshotEntity.builder()
                .candidateId(snapshot.candidateId())
                .symbol(snapshot.symbol())
                .observedAt(snapshot.observedAt())
                .regimeLabel(snapshot.regimeLabel().name())
                .regimeCompatibilityScore(snapshot.regimeCompatibilityScore())
                .asymmetryScore(snapshot.asymmetryScore())
                .equilibriumQualityScore(snapshot.equilibriumQualityScore())
                .reflexivityPotentialScore(snapshot.reflexivityPotentialScore())
                .deploymentConfidenceScore(snapshot.deploymentConfidenceScore())
                .reasonNotes(PersistenceStringUtils.joinText(snapshot.reasonNotes()))
                .build();
    }
}
