package dev.reddragon.app.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.CandidateReviewItem;
import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import lombok.RequiredArgsConstructor;

/**
 * Read-only review surface that lists the most recent PASS and WATCH candidates,
 * one row per candidate (de-duplicated to the latest verdict), ordered by score.
 *
 * <p>Example:
 * <pre>
 *   GET /api/review/candidates
 *   GET /api/review/candidates?verdicts=PASS
 *   GET /api/review/candidates?verdicts=PASS,WATCH&lookbackHours=48
 * </pre>
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class CandidateReviewController {

    private static final List<String> DEFAULT_VERDICTS = List.of("PASS", "WATCH");
    private static final int DEFAULT_LOOKBACK_HOURS = 24;

    private final ValidationVerdictRepository verdictRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;

    @GetMapping("/candidates")
    public List<CandidateReviewItem> listCandidates(
            @RequestParam(name = "verdicts", required = false) String verdictsParam,
            @RequestParam(name = "lookbackHours", defaultValue = "24") int lookbackHours
    ) {
        List<String> verdicts = parseVerdicts(verdictsParam);
        Instant since = Instant.now().minus(Math.max(1, lookbackHours), ChronoUnit.HOURS);

        return verdictRepository
                .findLatestPerCandidateSince(verdicts, since)
                .stream()
                .map(this::toReviewItem)
                .toList();
    }

    private List<String> parseVerdicts(String verdictsParam) {
        if (verdictsParam == null || verdictsParam.isBlank()) {
            return DEFAULT_VERDICTS;
        }
        return Arrays.stream(verdictsParam.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(v -> !v.isBlank())
                .toList();
    }

    private CandidateReviewItem toReviewItem(ValidationVerdictEntity entity) {
        String regimeLabel = analyticsSnapshotRepository
                .findTopByCandidateIdOrderByObservedAtDesc(entity.getCandidateId())
                .map(AnalyticsSnapshotEntity::getRegimeLabel)
                .orElse(null);

        return new CandidateReviewItem(
                entity.getCandidateId(),
                entity.getSymbol(),
                entity.getVerdict(),
                entity.getDeploymentTier(),
                entity.getScore(),
                splitReasonCodes(entity.getReasonCodes()),
                splitExplanations(entity.getExplanations()),
                regimeLabel,
                entity.getCreatedAt()
        );
    }

    private List<String> splitReasonCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<String> splitExplanations(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
