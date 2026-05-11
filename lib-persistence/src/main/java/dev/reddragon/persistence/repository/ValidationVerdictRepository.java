package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.ValidationVerdictEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationVerdictRepository extends JpaRepository<ValidationVerdictEntity, Long> {
    List<ValidationVerdictEntity> findTop25ByCandidateIdOrderByCreatedAtDesc(String candidateId);
    List<ValidationVerdictEntity> findTop25BySymbolOrderByCreatedAtDesc(String symbol);
}
