package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class SchwabTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_token", nullable = false, length = 4000)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 4000)
    private String refreshToken;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "token_type", nullable = false, length = 32)
    private String tokenType;
}
