package dev.reddragon.persistence.services.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.reddragon.persistence.domains.CalibrationReportEntity;

public interface CalibrationReportRepository extends JpaRepository<CalibrationReportEntity, Long> {

    List<CalibrationReportEntity> findTop100ByOrderByGeneratedAtDesc();
}
