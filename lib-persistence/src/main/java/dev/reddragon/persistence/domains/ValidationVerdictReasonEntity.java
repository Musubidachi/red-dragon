package dev.reddragon.persistence.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Normalized child row for {@link ValidationVerdictEntity}'s reasons.
 *
 * <p>Replaces the legacy comma/pipe-separated string blobs on
 * {@link ValidationVerdictEntity#getReasonCodes} and
 * {@link ValidationVerdictEntity#getExplanations}. Each row is one
 * {@link dev.reddragon.domain.models.ReasonCode}'s contribution to a verdict,
 * preserved in the order it was emitted by the validation engine via
 * {@code sortOrder} so the trader review surface can render them faithfully.
 *
 * <p>The L8 calibration analyses (see lib-analytics
 * {@code META_ADAPTATION_FEEDBACK.md} §3.1 "Dimension attribution") can now
 * query {@code SELECT verdict_id FROM validation_verdict_reason WHERE
 * reason_code = ?} without resorting to {@code LIKE} on a 4000-char blob.
 *
 * <p>Owns the {@code verdict_id} foreign-key column via a {@code @ManyToOne}
 * back-reference so Hibernate can populate the FK during cascade-save from
 * {@link ValidationVerdictEntity}. A small {@code verdictId()} convenience
 * method returns the parent's id without forcing callers to dereference the
 * entity.
 */
@Entity
@Table(name = "validation_verdict_reason")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ValidationVerdictReasonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verdict_id", nullable = false)
    @Setter   // mapper assigns the parent before save; LAZY back-reference avoids fetch-storms
    private ValidationVerdictEntity verdict;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    /**
     * Convenience: returns the parent verdict's id without forcing the caller
     * to dereference the entity. Returns {@code null} if the parent has not
     * been assigned yet (e.g. before the mapper attaches it).
     */
    public Long verdictId() {
        return verdict == null ? null : verdict.getId();
    }
}
