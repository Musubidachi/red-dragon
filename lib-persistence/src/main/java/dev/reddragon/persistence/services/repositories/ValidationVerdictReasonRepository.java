package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for normalized verdict-reason rows. Replaces the pattern of
 * {@code LIKE '%X%'} queries against the old comma-separated
 * {@code validation_verdict.reason_codes} column.
 *
 * <p>The {@code Verdict_Id} method naming uses Spring Data JPA's underscore
 * convention to navigate the {@code @ManyToOne verdict} relationship on
 * {@link ValidationVerdictReasonEntity} into its {@code id} field.
 */
public interface ValidationVerdictReasonRepository
        extends JpaRepository<ValidationVerdictReasonEntity, Long> {

    /**
     * All reason rows for a given verdict, in the order they were emitted by
     * the validation engine. Equivalent to splitting the legacy
     * {@code reason_codes} column on commas.
     */
    List<ValidationVerdictReasonEntity> findByVerdict_IdOrderBySortOrderAsc(Long verdictId);

    /**
     * All verdicts that fired a specific reason code. Indexed query;
     * intended for L8 calibration drift analysis (per
     * lib-analytics META_ADAPTATION_FEEDBACK.md §3.1).
     */
    List<ValidationVerdictReasonEntity> findByReasonCodeOrderByIdDesc(String reasonCode);

    /** Count occurrences of a specific reason code across all verdicts. */
    long countByReasonCode(String reasonCode);
}
