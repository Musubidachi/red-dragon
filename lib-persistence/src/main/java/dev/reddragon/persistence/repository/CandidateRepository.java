package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.CandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface CandidateRepository extends JpaRepository<CandidateEntity, String> {

    List<CandidateEntity> findTop25BySymbolOrderByObservedAtDesc(String symbol);

    /** Count candidates grouped by source type string (e.g., "SEC_EDGAR"). */
    long countBySourceType(String sourceType);

    /** Recent candidates for a specific catalyst type, newest-first. */
    List<CandidateEntity> findByCatalystTypeOrderByObservedAtDesc(String catalystType);

    /** Count candidates ingested after the given timestamp. */
    long countByObservedAtAfter(Instant since);

    /** Deduplication check — true if a candidate with the same source ID + type exists. */
    boolean existsBySourceIdAndSourceType(String sourceId, String sourceType);

    /** Paginated listing of all candidates, newest-first. */
    Page<CandidateEntity> findAllByOrderByObservedAtDesc(Pageable pageable);

    /** Paginated listing filtered by catalyst type, newest-first. */
    Page<CandidateEntity> findByCatalystTypeOrderByObservedAtDesc(String catalystType, Pageable pageable);

    /** Paginated listing filtered by source type, newest-first. */
    Page<CandidateEntity> findBySourceTypeOrderByObservedAtDesc(String sourceType, Pageable pageable);
}
