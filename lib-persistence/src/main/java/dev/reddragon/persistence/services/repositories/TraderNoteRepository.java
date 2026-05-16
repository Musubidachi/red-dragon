package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.TraderNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraderNoteRepository extends JpaRepository<TraderNoteEntity, Long> {

    /** All notes for a candidate, newest-first. */
    List<TraderNoteEntity> findByCandidateIdOrderByCreatedAtDesc(String candidateId);

    /** Count notes associated with a candidate. */
    long countByCandidateId(String candidateId);
}
