package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.Setter;

import java.time.Instant;

/**
 * Persistent record of the current Schwab OAuth access + refresh token pair.
 *
 * <p>One row per environment. The token service inserts a new row on every
 * refresh; the latest row by {@code issuedAt} is treated as authoritative.
 * Old rows are kept for audit only.
 */
@Entity
@Table(name = "schwab_token")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SchwabTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = SchwabTokenEncryptingConverter.class)
    @Column(name = "access_token", nullable = false, length = 8192)
    private String accessToken;

    @Convert(converter = SchwabTokenEncryptingConverter.class)
    @Column(name = "refresh_token", nullable = false, length = 8192)
    private String refreshToken;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "token_type", nullable = false, length = 32)
    private String tokenType;

    /** DB-populated insertion timestamp (V13); distinct from {@link #issuedAt}. */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    /** Last mutation timestamp (V13). Refreshes on token row updates. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-lock version (V13). Guards against concurrent refresh writes. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Builder constructor for new rows; generated IDs and audit metadata are persistence-managed. */
    @Builder
    public SchwabTokenEntity(
            String accessToken,
            String refreshToken,
            Instant issuedAt,
            Instant expiresAt,
            String tokenType
    ) {
        this(null, accessToken, refreshToken, issuedAt, expiresAt, tokenType,
                null, null, 0L);
    }

    /** Package-private legacy constructor for tests and migration fixtures. */
    SchwabTokenEntity(
            Long id,
            String accessToken,
            String refreshToken,
            Instant issuedAt,
            Instant expiresAt,
            String tokenType
    ) {
        this(id, accessToken, refreshToken, issuedAt, expiresAt, tokenType,
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
