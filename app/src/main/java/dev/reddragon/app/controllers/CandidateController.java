package dev.reddragon.app.controllers;

import dev.reddragon.app.models.CandidateListItemView;
import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paginated candidate listing with optional filtering.
 *
 * <pre>
 * GET /api/candidates?page=0&size=25
 * GET /api/candidates?catalystType=CONTRACT&page=0&size=25
 * GET /api/candidates?sourceType=SEC_EDGAR&page=0&size=25
 * </pre>
 */
@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CandidateRepository candidateRepository;

    @GetMapping
    public Page<CandidateListItemView> listCandidates(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false)    String catalystType,
            @RequestParam(required = false)    String sourceType
    ) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);

        if (catalystType != null && !catalystType.isBlank()) {
            return candidateRepository.findByCatalystTypeOrderByObservedAtDesc(
                    catalystType.trim().toUpperCase(), pageable).map(this::toView);
        }
        if (sourceType != null && !sourceType.isBlank()) {
            return candidateRepository.findBySourceTypeOrderByObservedAtDesc(
                    sourceType.trim().toUpperCase(), pageable).map(this::toView);
        }
        return candidateRepository.findAllByOrderByObservedAtDesc(pageable).map(this::toView);
    }

    private CandidateListItemView toView(CandidateEntity candidate) {
        return new CandidateListItemView(
                candidate.getCandidateId(),
                candidate.getSymbol(),
                candidate.getCompanyName(),
                candidate.getCatalystType(),
                candidate.getSourceType(),
                candidate.getSourceId(),
                candidate.getSourceUrl(),
                candidate.getObservedAt(),
                candidate.getHeadline(),
                candidate.getSummary()
        );
    }
}
