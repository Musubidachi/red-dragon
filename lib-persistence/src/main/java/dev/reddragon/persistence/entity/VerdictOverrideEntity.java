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

/**
 * Persisted representation of a trader-supplied override of a validation verdict.
 *
 * <p>The original verdict is preserved for audit purposes; both the original and
 * the override verdict are stored as strings so the entity does not depend on
 * validation library enum types.
 */
@Entity
@Table(name = "verdict_override")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VerdictOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "verdict_id", nullable = false)
    private Long verdictId;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "original_verdict", nullable = false, length = 32)
    private String originalVerdict;

    @Column(name = "override_verdict", nullable = false, length = 32)
    private String overrideVerdict;

    @Column(name = "reason", length = 1024)
    private String reason;

    @Column(name = "overridden_at", nullable = false)
    private Instant overriddenAt;

    @Column(name = "author", length = 128)
    private String author;
}
