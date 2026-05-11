package dev.reddragon.persistence.repository;

import dev.reddragon.persistence.entity.CandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<CandidateEntity, String> {
    List<CandidateEntity> findTop25BySymbolOrderByObservedAtDesc(String symbol);
}
