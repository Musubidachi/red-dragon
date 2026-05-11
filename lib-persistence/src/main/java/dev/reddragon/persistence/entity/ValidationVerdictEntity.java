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
@Table(name = "validation_verdict")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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
}
