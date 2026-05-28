package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "overridden_at", nullable = false)
    private Instant overriddenAt;

    @Column(name = "author", length = 128)
    private String author;

    /** DB-managed insertion timestamp (V13). */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Last mutation timestamp (V13) — refreshed by {@link #touchUpdatedAt()}. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-lock version (V13). */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Builder constructor for new rows; generated IDs and audit metadata are persistence-managed. */
    @Builder
    public VerdictOverrideEntity(
            Long verdictId,
            String candidateId,
            String symbol,
            String originalVerdict,
            String overrideVerdict,
            String reason,
            Instant overriddenAt,
            String author
    ) {
        this(null, verdictId, candidateId, symbol, originalVerdict, overrideVerdict,
                reason, overriddenAt, author, null, null, 0L);
    }

    /** Package-private legacy constructor for tests and migration fixtures. */
    VerdictOverrideEntity(
            Long id,
            Long verdictId,
            String candidateId,
            String symbol,
            String originalVerdict,
            String overrideVerdict,
            String reason,
            Instant overriddenAt,
            String author
    ) {
        this(id, verdictId, candidateId, symbol, originalVerdict, overrideVerdict,
                reason, overriddenAt, author, null, null, 0L);
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
