package dev.reddragon.persistence.services.repositories;

import dev.reddragon.persistence.domains.SchwabTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchwabTokenRepository extends JpaRepository<SchwabTokenEntity, Long> {

    /** Latest persisted token row, or {@code null} if none has ever been stored. */
    SchwabTokenEntity findTopByOrderByIssuedAtDesc();
}
