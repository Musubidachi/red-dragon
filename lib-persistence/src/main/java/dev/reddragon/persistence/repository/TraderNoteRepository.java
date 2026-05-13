package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.TraderNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraderNoteRepository extends JpaRepository<TraderNoteEntity, Long> {

    /** All notes for a candidate, newest-first. */
    List<TraderNoteEntity> findByCandidateIdOrderByCreatedAtDesc(String candidateId);

    /** Count notes associated with a candidate. */
    long countByCandidateId(String candidateId);
}
