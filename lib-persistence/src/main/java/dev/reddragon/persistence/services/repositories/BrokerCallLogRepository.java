package dev.reddragon.persistence.services.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.BrokerCallLogEntity;

public interface BrokerCallLogRepository extends JpaRepository<BrokerCallLogEntity, Long> {

    List<BrokerCallLogEntity> findByRecordedAtAfterOrderByRecordedAtDesc(Instant recordedAt, Pageable pageable);
}
