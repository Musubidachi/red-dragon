package dev.reddragon.app.api;

import dev.reddragon.persistence.entity.CandidateEntity;
import dev.reddragon.persistence.entity.ValidationVerdictEntity;
import dev.reddragon.persistence.repository.CandidateRepository;
import dev.reddragon.persistence.repository.ValidationVerdictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return response;
    }

    private CandidateReviewItem toReviewItem(ValidationVerdictEntity entity) {
        return new CandidateReviewItem(
                entity.getCandidateId(),
                entity.getSymbol(),
                entity.getVerdict(),
                entity.getDeploymentTier(),
                entity.getScore(),
                split(entity.getReasonCodes(), ","),
                split(entity.getExplanations(), "\\|"),
                null,
                entity.getCreatedAt()
        );
    }

    private List<String> split(String raw, String delimiter) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(delimiter))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
