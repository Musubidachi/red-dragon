package dev.reddragon.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "validation_verdict")
public class ValidationVerdictEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "verdict", nullable = false, length = 32)
    private String verdict;

    @Column(name = "deployment_tier", nullable = false, length = 32)
    private String deploymentTier;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "reason_codes", length = 4000)
    private String reasonCodes;

    @Column(name = "explanations", length = 8000)
    private String explanations;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ValidationVerdictEntity() {
    }

    public ValidationVerdictEntity(
            String candidateId,
            String symbol,
            String verdict,
            String deploymentTier,
            double score,
            String reasonCodes,
            String explanations,
            Instant createdAt
    ) {
        this.candidateId = candidateId;
        this.symbol = symbol;
        this.verdict = verdict;
        this.deploymentTier = deploymentTier;
        this.score = score;
        this.reasonCodes = reasonCodes;
        this.explanations = explanations;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getVerdict() {
        return verdict;
    }

    public String getDeploymentTier() {
        return deploymentTier;
    }

    public double getScore() {
        return score;
    }

    public String getReasonCodes() {
        return reasonCodes;
    }

    public String getExplanations() {
        return explanations;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
