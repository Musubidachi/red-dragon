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
@Table(name = "analytics_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AnalyticsSnapshotEntity {

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

    @Column(name = "regime_label", nullable = false, length = 64)
    private String regimeLabel;

    @Column(name = "regime_compatibility_score", nullable = false)
    private double regimeCompatibilityScore;

    @Column(name = "asymmetry_score", nullable = false)
    private double asymmetryScore;

    @Column(name = "equilibrium_quality_score", nullable = false)
    private double equilibriumQualityScore;

    @Column(name = "reflexivity_potential_score", nullable = false)
    private double reflexivityPotentialScore;

    @Column(name = "deployment_confidence_score", nullable = false)
    private double deploymentConfidenceScore;

    @Column(name = "reason_notes", length = 4000)
    private String reasonNotes;
}
