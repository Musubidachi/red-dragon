package dev.reddragon.app.controllers;

import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.validation.models.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Platform-wide summary statistics.
 *
 * <pre>
 * GET /api/stats?lookbackHours=24
 * </pre>
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final CandidateRepository candidateRepository;
    private final ValidationVerdictRepository validationVerdictRepository;

    @GetMapping
    public Map<String, Object> stats(
            @RequestParam(defaultValue = "24") int lookbackHours
    ) {
        Instant since = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);

        long totalCandidates = candidateRepository.count();
        long recentCandidates = candidateRepository.countByObservedAtAfter(since);

        long totalVerdicts = validationVerdictRepository.count();
        long recentPasses   = validationVerdictRepository.countByVerdictAndCreatedAtAfter(Verdict.PASS.name(), since);
        long recentWatches  = validationVerdictRepository.countByVerdictAndCreatedAtAfter(Verdict.WATCH.name(), since);
        long recentRejects  = validationVerdictRepository.countByVerdictAndCreatedAtAfter(Verdict.REJECT.name(), since);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackHours", lookbackHours);
        result.put("totalCandidates", totalCandidates);
        result.put("recentCandidates", recentCandidates);
        result.put("totalVerdicts", totalVerdicts);
        result.put("recentPasses", recentPasses);
        result.put("recentWatches", recentWatches);
        result.put("recentRejects", recentRejects);
        result.put("generatedAt", Instant.now().toString());
        return result;
    }
}
