package dev.reddragon.app.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.CandidateReviewItem;
import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.MarketSnapshotRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import lombok.RequiredArgsConstructor;

/**
 * Read-only history endpoints for candidates and their validation records.
 *
 * <pre>
 *   GET /api/candidates/{symbol}/history
 *   GET /api/candidates/id/{candidateId}
 * </pre>
 */
@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateHistoryController {

    private final CandidateRepository candidateRepository;
    private final ValidationVerdictRepository verdictRepository;
    private final MarketSnapshotRepository marketSnapshotRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final TraderNoteRepository traderNoteRepository;
    private final VerdictOverrideRepository overrideRepository;

    /**
     * All validation verdicts for a symbol, newest-first.
     * The optional {@code limit} param caps the result. Repository method is
     * currently limited to 25 rows, so requests above 25 still return 25.
     */
    @GetMapping("/{symbol}/history")
    public List<CandidateReviewItem> symbolHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "25") int limit
    ) {
        int safeLimit = Math.min(Math.max(1, limit), 25);
        return verdictRepository.findTop25BySymbolOrderByCreatedAtDesc(symbol.trim().toUpperCase())
                .stream()
                .limit(safeLimit)
                .map(this::toReviewItem)
                .toList();
    }

    /**
     * Single candidate lookup with its recent validation records.
     * Returns 404 if the candidate is not found.
     */
    @GetMapping("/id/{candidateId}")
    public ResponseEntity<Map<String, Object>> candidateDetail(
            @PathVariable String candidateId
    ) {
        return candidateRepository.findById(candidateId)
                .map(candidate -> ResponseEntity.ok(toCandidateDetail(candidate)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> toCandidateDetail(CandidateEntity candidate) {
        List<CandidateReviewItem> verdicts = verdictRepository
                .findTop25ByCandidateIdOrderByCreatedAtDesc(candidate.getCandidateId())
                .stream()
                .map(this::toReviewItem)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("candidateId", candidate.getCandidateId());
        response.put("symbol", candidate.getSymbol());
        response.put("companyName", nullToEmpty(candidate.getCompanyName()));
        response.put("catalystType", candidate.getCatalystType());
        response.put("sourceType", candidate.getSourceType());
        response.put("headline", nullToEmpty(candidate.getHeadline()));
        response.put("summary", nullToEmpty(candidate.getSummary()));
        response.put("observedAt", candidate.getObservedAt());
        response.put("verdicts", verdicts);
        response.put("marketSnapshots", marketSnapshotRepository
                .findTop25ByCandidateIdOrderByObservedAtDesc(candidate.getCandidateId()));
        response.put("analyticsSnapshots", analyticsSnapshotRepository
                .findTop25ByCandidateIdOrderByObservedAtDesc(candidate.getCandidateId()));
        response.put("notes", traderNoteRepository
                .findByCandidateIdOrderByCreatedAtDesc(candidate.getCandidateId()));
        response.put("overrides", overrideRepository
                .findByCandidateIdOrderByOverriddenAtDesc(candidate.getCandidateId()));
        return response;
    }

    private CandidateReviewItem toReviewItem(ValidationVerdictEntity entity) {
        return new CandidateReviewItem(
                entity.getCandidateId(),
                entity.getSymbol(),
                entity.getVerdict(),
                entity.getDeploymentTier(),
                entity.getScore(),
                reasonCodes(entity),
                explanations(entity),
                null,
                entity.getCreatedAt()
        );
    }

    /**
     * Reason codes from the normalized {@code validation_verdict_reason}
     * child table (post-V12 schema). Reads from {@code entity.getReasons()}
     * rather than splitting the legacy comma-separated blob.
     */
    private List<String> reasonCodes(ValidationVerdictEntity entity) {
        if (entity.getReasons() == null || entity.getReasons().isEmpty()) {
            return List.of();
        }
        return entity.getReasons().stream()
                .map(ValidationVerdictReasonEntity::getReasonCode)
                .toList();
    }

    /**
     * Explanations from the normalized child table, in the same order as
     * the reason codes. {@code null}/blank entries are filtered out.
     */
    private List<String> explanations(ValidationVerdictEntity entity) {
        if (entity.getReasons() == null || entity.getReasons().isEmpty()) {
            return List.of();
        }
        return entity.getReasons().stream()
                .map(ValidationVerdictReasonEntity::getExplanation)
                .filter(e -> e != null && !e.isBlank())
                .toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
