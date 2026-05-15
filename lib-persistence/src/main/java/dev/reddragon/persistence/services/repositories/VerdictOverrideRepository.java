package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.VerdictOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VerdictOverrideRepository extends JpaRepository<VerdictOverrideEntity, Long> {

    /** Most recent override for a candidate, if any. */
    Optional<VerdictOverrideEntity> findTopByCandidateIdOrderByOverriddenAtDesc(String candidateId);

    /** All overrides for a candidate, newest-first. */
    List<VerdictOverrideEntity> findByCandidateIdOrderByOverriddenAtDesc(String candidateId);

    /** True if a candidate has been manually overridden at least once. */
    boolean existsByCandidateId(String candidateId);
}
