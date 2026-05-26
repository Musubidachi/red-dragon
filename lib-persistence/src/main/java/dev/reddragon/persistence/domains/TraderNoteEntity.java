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
 * Persisted representation of a trader annotation on a candidate.
 */
@Entity
@Table(name = "trader_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TraderNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "candidate_id", nullable = false, length = 64)
    private String candidateId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "note_text", nullable = false, length = 4000)
    private String noteText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "author", length = 128)
    private String author;

    /** Last mutation timestamp (V13). Note edits refresh this. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-lock version (V13). */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Backwards-compatible pre-V13 6-arg constructor. */
    public TraderNoteEntity(
            Long id,
            String candidateId,
            String symbol,
            String noteText,
            Instant createdAt,
            String author
    ) {
        this(id, candidateId, symbol, noteText, createdAt, author, null, 0L);
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
