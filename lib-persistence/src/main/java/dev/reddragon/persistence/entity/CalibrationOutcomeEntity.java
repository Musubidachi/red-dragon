package dev.reddragon.persistence.entity;

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
}
