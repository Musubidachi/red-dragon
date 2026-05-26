package dev.reddragon.persistence.domains;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "calibration_outcome")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalibrationOutcomeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String candidateId;
    @Column(nullable = false)
    private String symbol;
    @Column(nullable = false)
    private Instant observedAt;

    private double structuralRealityScore;
    private double materialSignificanceScore;
    private double earlynessScore;
    private double equilibriumQualityScore;
    private double reflexivityPotentialScore;
    private double asymmetryScore;
    private double regimeCompatibilityScore;
    private double deploymentConfidenceScore;

    private double realizedReturn;
    private double maxDrawdown;
    private int daysHeld;
    private boolean thesisWorked;

    /** DB-populated insertion timestamp (V13). */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Backwards-compatible pre-V13 constructor. */
    public CalibrationOutcomeEntity(
            Long id,
            String candidateId,
            String symbol,
            Instant observedAt,
            double structuralRealityScore,
            double materialSignificanceScore,
            double earlynessScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double asymmetryScore,
            double regimeCompatibilityScore,
            double deploymentConfidenceScore,
            double realizedReturn,
            double maxDrawdown,
            int daysHeld,
            boolean thesisWorked
    ) {
        this(id, candidateId, symbol, observedAt,
                structuralRealityScore, materialSignificanceScore, earlynessScore,
                equilibriumQualityScore, reflexivityPotentialScore, asymmetryScore,
                regimeCompatibilityScore, deploymentConfidenceScore,
                realizedReturn, maxDrawdown, daysHeld, thesisWorked,
                null);
    }
}
