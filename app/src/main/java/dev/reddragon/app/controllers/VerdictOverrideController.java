package dev.reddragon.app.controllers;

import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.VerdictOverrideEntity;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import dev.reddragon.domain.models.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Trader-supplied manual overrides for validation verdicts.
 *
 * <pre>
 * POST /api/verdicts/{id}/override
 * Body: {"verdict": "PASS", "reason": "...", "author": "..."}
 * </pre>
 */
@RestController
@RequestMapping("/api/verdicts")
@RequiredArgsConstructor
public class VerdictOverrideController {

    private final ValidationVerdictRepository verdictRepository;
    private final VerdictOverrideRepository overrideRepository;

    @PostMapping("/{id}/override")
    public ResponseEntity<VerdictOverrideEntity> override(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        ValidationVerdictEntity verdict = verdictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Verdict not found: " + id));

        String overrideVerdictRaw = body.get("verdict");
        if (overrideVerdictRaw == null || overrideVerdictRaw.isBlank()) {
            throw new IllegalArgumentException("verdict must not be blank");
        }

        // Validate the override value
        Verdict overrideVerdict;
        try {
            overrideVerdict = Verdict.valueOf(overrideVerdictRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown verdict: " + overrideVerdictRaw);
        }

        VerdictOverrideEntity override = new VerdictOverrideEntity(
                null,
                verdict.getId(),
                verdict.getCandidateId(),
                verdict.getSymbol(),
                verdict.getVerdict(),
                overrideVerdict.name(),
                body.get("reason"),
                Instant.now(),
                body.get("author")
        );
        return ResponseEntity.ok(overrideRepository.save(override));
    }
}
