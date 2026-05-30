package dev.reddragon.persistence.domains;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_state_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MarketStateSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Column(name = "regime_label", nullable = false, length = 64)
    private String regimeLabel;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "equilibrium_restoration_probability", nullable = false)
    private double equilibriumRestorationProbability;

    @Column(name = "deployment_supported", nullable = false)
    private boolean deploymentSupported;

    @Column(name = "session_vwap", nullable = false)
    private double sessionVwap;

    @Column(name = "latest_close", nullable = false)
    private double latestClose;

    @Column(name = "vwap_distance_percent", nullable = false)
    private double vwapDistancePercent;

    @Column(name = "vwap_reclaim_strength", nullable = false)
    private double vwapReclaimStrength;

    @Column(name = "directional_persistence_score", nullable = false)
    private double directionalPersistenceScore;

    @Column(name = "rotational_quality_score", nullable = false)
    private double rotationalQualityScore;

    @Column(name = "intraday_trend_strength", nullable = false)
    private double intradayTrendStrength;

    @Column(name = "above_vwap", nullable = false)
    private boolean aboveVwap;

    @Column(name = "bar_count", nullable = false)
    private int barCount;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public MarketStateSnapshotEntity(
            String symbol,
            Instant observedAt,
            String source,
            String regimeLabel,
            double confidence,
            double equilibriumRestorationProbability,
            boolean deploymentSupported,
            double sessionVwap,
            double latestClose,
            double vwapDistancePercent,
            double vwapReclaimStrength,
            double directionalPersistenceScore,
            double rotationalQualityScore,
            double intradayTrendStrength,
            boolean aboveVwap,
            int barCount,
            String notes
    ) {
        this(null, symbol, observedAt, source, regimeLabel, confidence,
                equilibriumRestorationProbability, deploymentSupported, sessionVwap,
                latestClose, vwapDistancePercent, vwapReclaimStrength,
                directionalPersistenceScore, rotationalQualityScore,
                intradayTrendStrength, aboveVwap, barCount, notes, null);
    }
}
