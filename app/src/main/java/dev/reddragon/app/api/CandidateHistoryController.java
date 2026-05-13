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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only history endpoints for candidates and their validation records.
 *
 * <pre>
 *   GET /api/candidates/{symbol}/history           — all verdicts for a symbol
 *   GET /api/candidates/{candidateId}              — one candidate + its verdicts
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
     * The optional {@code limit} param caps the result (default 25, max 100).
     */
    @GetMapping("/{symbol}/history")
    public List<CandidateReviewItem> symbolHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "25") int limit
    ) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        return verdictRepository.findTop25BySymbolOrderByCreatedAtDesc(symbol.toUpperCase())
                .stream()
                .limit(safeLimit)
                .map(this::toReviewItem)
                .toList();
    }

    /**
     * Single candidate lookup with its full validation record.
     * Returns 404 if the candidate is not found.
     */
    @GetMapping("/id/{candidateId}")
    public ResponseEntity<Map<String, Object>> candidateDetail(
            @PathVariable String candidateId
    ) {
        Optional<CandidateEntity> candidate = candidateRepository.findById(candidateId);
        if (candidate.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<CandidateReviewItem> verdicts = verdictRepository
                .findTop25ByCandidateIdOrderByCreatedAtDesc(candidateId)
                .stream()
                .map(this::toReviewItem)
                .toList();

        CandidateEntity c = candidate.get();
        Map<String, Object> response = Map.of(
                "candidateId", c.getCandidateId(),
                "symbol", c.getSymbol(),
                "companyName", c.getCompanyName() == null ? "" : c.getCompanyName(),
                "catalystType", c.getCatalystType(),
                "sourceType", c.getSourceType(),
                "headline", c.getHeadline() == null ? "" : c.getHeadline(),
                "summary", c.getSummary() == null ? "" : c.getSummary(),
                "observedAt", c.getObservedAt().toString(),
                "verdicts", verdicts
        );
        return ResponseEntity.ok(response);
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
}
