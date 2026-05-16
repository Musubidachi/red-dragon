package dev.reddragon.persistence.services.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.TradeHistoryImportBatchEntity;

public interface TradeHistoryImportBatchRepository extends JpaRepository<TradeHistoryImportBatchEntity, Long> {

    List<TradeHistoryImportBatchEntity> findTop25ByOrderByImportedAtDesc();
}
