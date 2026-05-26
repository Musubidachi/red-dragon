package dev.reddragon.app.controllers;

import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Human-readable validation summary for a specific candidate.
 *
 * <pre>
 * GET /api/verdicts/{candidateId}/summary
 * </pre>
 *
 * Returns the most recent verdict plus all historical entries and any
 * manual override on record for the candidate.
 */
@RestController
@RequestMapping("/api/verdicts")
@RequiredArgsConstructor
public class VerdictSummaryController {

    private final ValidationVerdictRepository verdictRepository;
    private final VerdictOverrideRepository overrideRepository;

    @GetMapping("/{candidateId}/summary")
    public ResponseEntity<Map<String, Object>> summary(@PathVariable String candidateId) {
        List<ValidationVerdictEntity> history =
                verdictRepository.findTop25ByCandidateIdOrderByCreatedAtDesc(candidateId);

        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ValidationVerdictEntity latest = history.get(0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("candidateId",     candidateId);
        result.put("symbol",          latest.getSymbol());
        result.put("latestVerdict",   latest.getVerdict());
        result.put("latestScore",     latest.getScore());
        result.put("deploymentTier",  latest.getDeploymentTier());
        // Read reason codes from the V12 normalized child table.
        result.put("reasonCodes",     latest.getReasons() == null
                ? List.of()
                : latest.getReasons().stream()
                        .map(r -> r.getReasonCode())
                        .toList());
        result.put("verdictCount",    history.size());
        result.put("lastEvaluated",   latest.getCreatedAt().toString());

        overrideRepository.findTopByCandidateIdOrderByOverriddenAtDesc(candidateId)
                .ifPresent(o -> {
                    Map<String, Object> override = new LinkedHashMap<>();
                    override.put("overrideVerdict", o.getOverrideVerdict());
                    override.put("reason",          o.getReason());
                    override.put("author",          o.getAuthor());
                    override.put("overriddenAt",    o.getOverriddenAt().toString());
                    result.put("override", override);
                });

        return ResponseEntity.ok(result);
    }
}
