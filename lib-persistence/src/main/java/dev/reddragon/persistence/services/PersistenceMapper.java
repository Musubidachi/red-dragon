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

import java.time.Instant;
import java.util.ArrayList;
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
                .reasonCodes(null)   // legacy column — no longer written (V12+)
                .explanations(null)  // legacy column — no longer written (V12+)
                .createdAt(Instant.now())
                .reasons(new ArrayList<>())
                .build();
        entity.getReasons().addAll(buildReasonEntities(entity, result));
        return entity;
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
            ValidationVerdictReasonEntity child = new ValidationVerdictReasonEntity(
                    null,            // id — generated
                    parent,          // back-reference; Hibernate fills verdict_id from parent.id on cascade-save
                    code,
                    explanation,
                    (short) i
            );
            children.add(child);
        }
        return children;
    }

    public MarketBarEntity toMarketBarEntity(MarketBar bar) {
        Objects.requireNonNull(bar, "bar is required");

        return new MarketBarEntity(
                null,
                bar.symbol(),
                bar.date(),
                bar.open(),
                bar.high(),
                bar.low(),
                bar.close(),
                bar.volume(),
                null   // createdAt — populated by DB default (V11)
        );
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

        return new IntradayBarEntity(
                null,
                bar.symbol(),
                bar.startTime(),
                bar.open(),
                bar.high(),
                bar.low(),
                bar.close(),
                bar.volume(),
                bar.vwap(),
                Instant.now()
        );
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

        return new MarketQuoteObservationEntity(
                null,
                quote.symbol(),
                quote.observedAt(),
                quote.lastPrice(),
                quote.bidPrice(),
                quote.askPrice(),
                quote.volume(),
                quote.quality().name(),
                PersistenceStringUtils.joinText(quote.notes())
        );
    }

    public MarketSnapshotEntity toMarketSnapshotEntity(String candidateId, MarketDataSnapshot snapshot) {
        Objects.requireNonNull(candidateId, "candidateId is required");
        Objects.requireNonNull(snapshot, "snapshot is required");

        return new MarketSnapshotEntity(
                null,
                candidateId,
                snapshot.symbol(),
                snapshot.observedAt(),
                snapshot.latestClose(),
                snapshot.previousClose(),
                snapshot.gapPercent(),
                snapshot.averageTrueRange(),
                snapshot.rangePosition(),
                snapshot.averageVolume(),
                snapshot.liquidityScore(),
                snapshot.volatilityStabilityScore(),
                snapshot.quality().name(),
                PersistenceStringUtils.joinText(snapshot.notes()),
                snapshot.relativeVolume(),
                snapshot.vwapDeviation(),
                snapshot.directionalPersistence()
        );
    }

    public AnalyticsSnapshotEntity toAnalyticsSnapshotEntity(AnalyticsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "analytics snapshot is required");

        return new AnalyticsSnapshotEntity(
                null,
                snapshot.candidateId(),
                snapshot.symbol(),
                snapshot.observedAt(),
                snapshot.regimeLabel().name(),
                snapshot.regimeCompatibilityScore(),
                snapshot.asymmetryScore(),
                snapshot.equilibriumQualityScore(),
                snapshot.reflexivityPotentialScore(),
                snapshot.deploymentConfidenceScore(),
                PersistenceStringUtils.joinText(snapshot.reasonNotes())
        );
    }
}
