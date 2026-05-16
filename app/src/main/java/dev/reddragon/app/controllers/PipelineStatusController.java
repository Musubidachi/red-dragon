package dev.reddragon.app.controllers;

import dev.reddragon.persistence.services.repositories.BacktestResultRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight operational status check for the pipeline.
 *
 * <pre>
 * GET /api/pipeline/status
 * </pre>
 *
 * Returns counts that confirm the persistence layer is reachable and the
 * pipeline has been exercised at least once.  This is distinct from the
 * actuator {@code /actuator/health} endpoint which checks infrastructure.
 */
@RestController
@RequestMapping("/api/pipeline/status")
@RequiredArgsConstructor
public class PipelineStatusController {

    private final CandidateRepository candidateRepository;
    private final ValidationVerdictRepository verdictRepository;
    private final BacktestResultRepository backtestResultRepository;

    @GetMapping
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("candidateCount", candidateRepository.count());
        result.put("verdictCount", verdictRepository.count());
        result.put("backtestResultCount", backtestResultRepository.count());
        result.put("checkedAt", Instant.now().toString());
        return result;
    }
}
