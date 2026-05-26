package dev.reddragon.persistence.domains;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "validation_verdict")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    /**
     * Deterministic semantic fingerprint for idempotent verdict writes.
     * Historical rows may be null; new writes are populated by
     * {@code PersistenceMapper}.
     */
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    /**
     * @deprecated as of V12. Reasons live in {@code validation_verdict_reason}
     * as one row per code; this column is retained nullable for backfill
     * compatibility with rows written before V12 but is no longer populated
     * on writes. Use {@link #getReasons()} or {@link #legacyReasonCodes()}.
     */
    @Deprecated(forRemoval = true)
    @Column(name = "reason_codes", length = 4000)
    private String reasonCodes;

    /**
     * @deprecated as of V12. See {@link #reasonCodes}. Explanations now live
     * per-row in {@code validation_verdict_reason.explanation}. Use
     * {@link #getReasons()} or {@link #legacyExplanations()}.
     */
    @Deprecated(forRemoval = true)
    @Column(name = "explanations", length = 8000)
    private String explanations;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Normalized reasons + explanations for this verdict, in the order the
     * validation engine emitted them.
     *
     * <p>Bidirectional one-to-many: child entities hold a {@code @ManyToOne}
     * back-reference at {@link ValidationVerdictReasonEntity#getVerdict}.
     * {@link CascadeType#ALL} + orphan-removal means the mapper can construct
     * the parent with reasons attached (each child's parent reference set to
     * {@code this}) and a single {@code save(entity)} call persists both.
     * The FK is also enforced at the DB level by V12's
     * {@code fk_validation_verdict_reason_verdict} with
     * {@code on delete cascade}.
     */
    @OneToMany(
            mappedBy = "verdict",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("sortOrder ASC")
    private List<ValidationVerdictReasonEntity> reasons = new ArrayList<>();

    /**
     * Wall-clock instant of the most recent UPDATE — refreshed by
     * {@link #touchUpdatedAt()}. Verdicts are typically immutable after
     * write, but the column exists for the rare re-scoring path and so
     * the audit pattern is uniform across mutable entities.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-lock version (V13). */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * Backwards-compatible factory constructor (pre-V13 shape).
     */
    public ValidationVerdictEntity(
            Long id,
            String candidateId,
            String symbol,
            String verdict,
            String deploymentTier,
            double score,
            String reasonCodes,
            String explanations,
            Instant createdAt,
            List<ValidationVerdictReasonEntity> reasons
    ) {
        this(id, candidateId, symbol, verdict, deploymentTier, score,
                null, reasonCodes, explanations, createdAt,
                reasons == null ? new ArrayList<>() : reasons,
                null, 0L);
    }

    /**
     * Joined string of reason-code names, comma-separated. Provided for
     * controllers and serializers that still expect the legacy shape;
     * prefer reading {@link #getReasons()} directly.
     */
    public String legacyReasonCodes() {
        return reasons == null || reasons.isEmpty()
                ? ""
                : reasons.stream()
                        .map(ValidationVerdictReasonEntity::getReasonCode)
                        .collect(Collectors.joining(","));
    }

    /**
     * Joined string of explanation lines, pipe-separated. Mirrors
     * {@link #legacyReasonCodes()} for the explanation field.
     */
    public String legacyExplanations() {
        return reasons == null || reasons.isEmpty()
                ? ""
                : reasons.stream()
                        .map(ValidationVerdictReasonEntity::getExplanation)
                        .filter(e -> e != null && !e.isBlank())
                        .collect(Collectors.joining(" | "));
    }

    @PrePersist
    void onInsert() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = Instant.now();
    }
}
