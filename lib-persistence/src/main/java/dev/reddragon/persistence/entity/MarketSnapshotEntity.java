package dev.reddragon.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "market_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MarketSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "latest_close", nullable = false)
    private double latestClose;

    @Column(name = "previous_close", nullable = false)
    private double previousClose;

    @Column(name = "gap_percent", nullable = false)
    private double gapPercent;

    @Column(name = "average_true_range", nullable = false)
    private double averageTrueRange;

    @Column(name = "range_position", nullable = false)
    private double rangePosition;

    @Column(name = "average_volume", nullable = false)
    private double averageVolume;

    @Column(name = "liquidity_score", nullable = false)
    private double liquidityScore;

    @Column(name = "volatility_stability_score", nullable = false)
    private double volatilityStabilityScore;

    @Column(name = "quality", nullable = false, length = 64)
    private String quality;

    @Column(name = "notes", length = 4000)
    private String notes;

    /** Ratio of latest bar volume to period average volume (nullable for old rows). */
    @Column(name = "relative_volume")
    private Double relativeVolume;

    /** (latestClose − VWAP) / latestClose (nullable for old rows). */
    @Column(name = "vwap_deviation")
    private Double vwapDeviation;

    /** Fraction of bars with consistent directional momentum (nullable for old rows). */
    @Column(name = "directional_persistence")
    private Double directionalPersistence;
}
