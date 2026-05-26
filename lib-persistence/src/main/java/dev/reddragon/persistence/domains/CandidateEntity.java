package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "candidate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CandidateEntity {
    @Id
    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "company_name", length = 256)
    private String companyName;

    @Column(name = "catalyst_type", nullable = false, length = 64)
    private String catalystType;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_id", length = 256)
    private String sourceId;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "headline", length = 512)
    private String headline;

    @Column(name = "summary", length = 4000)
    private String summary;

    /**
     * Wall-clock instant the row was inserted into the database. Distinct
     * from {@link #observedAt} (when the catalyst happened in the outside
     * world) — the gap measures ingestion latency. Populated by the V13
     * DB default; never written from Java.
     */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /**
     * Wall-clock instant of the most recent UPDATE. Refreshed by
     * {@link #touchUpdatedAt()} on every mutation; seeded by the V13
     * default for backfilled rows.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic-lock version (V13). Hibernate increments on every UPDATE;
     * a stale write throws {@link jakarta.persistence.OptimisticLockException}
     * rather than clobbering a concurrent edit.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * Backwards-compatible factory constructor — keeps existing callers
     * (PersistenceMapper, controllers) on the original 10-arg shape. The
     * three V13 audit columns are JPA-managed: {@code createdAt} is
     * populated by the DB default, {@code updatedAt} by {@link #onInsert},
     * and {@code version} starts at 0 for new rows.
     */
    public CandidateEntity(
            String candidateId,
            String symbol,
            String companyName,
            String catalystType,
            String sourceType,
            String sourceId,
            String sourceUrl,
            Instant observedAt,
            String headline,
            String summary
    ) {
        this(candidateId, symbol, companyName, catalystType, sourceType, sourceId,
                sourceUrl, observedAt, headline, summary,
                null, null, 0L);
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
