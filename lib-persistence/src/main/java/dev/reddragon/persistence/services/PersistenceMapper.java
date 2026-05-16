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
import dev.reddragon.persistence.utilities.PersistenceStringUtils;
import dev.reddragon.domain.models.ValidationResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Maps pipeline domain objects into persistence entities.
 */
public class PersistenceMapper {

    public CandidateEntity toCandidateEntity(TradeCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate is required");

        return new CandidateEntity(
                candidate.candidateId(),
                candidate.symbol(),
                candidate.companyName(),
                candidate.catalystType().name(),
                candidate.sourceType().name(),
                candidate.sourceId(),
                candidate.sourceUrl(),
                candidate.observedAt(),
                candidate.headline(),
                candidate.summary()
        );
    }

    public ValidationVerdictEntity toValidationVerdictEntity(ValidationResult result) {
        Objects.requireNonNull(result, "validation result is required");

        return new ValidationVerdictEntity(
                null,
                result.candidateId(),
                result.symbol(),
                result.verdict().name(),
                result.deploymentTier().name(),
                result.score(),
                PersistenceStringUtils.joinNames(result.reasonCodes()),
                PersistenceStringUtils.joinText(result.explanations()),
                Instant.now()
        );
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
                bar.volume()
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
