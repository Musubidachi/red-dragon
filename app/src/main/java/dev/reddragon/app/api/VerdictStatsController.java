package dev.reddragon.app.api;

import dev.reddragon.persistence.repository.ValidationVerdictRepository;
import dev.reddragon.validation.model.DeploymentTier;
import dev.reddragon.validation.model.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verdict distribution and deployment tier statistics.
 *
 * <pre>
 * GET /api/verdicts/stats?lookbackHours=24
 * </pre>
 */
@RestController
@RequestMapping("/api/verdicts/stats")
@RequiredArgsConstructor
public class VerdictStatsController {

    private final ValidationVerdictRepository verdictRepository;

    @GetMapping
    public Map<String, Object> stats(
            @RequestParam(defaultValue = "24") int lookbackHours
    ) {
        Instant since = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);

        // Verdict counts
        Map<String, Long> verdictCounts = Arrays.stream(Verdict.values())
                .collect(Collectors.toMap(
                        Verdict::displayName,
                        v -> verdictRepository.countByVerdictAndCreatedAtAfter(v.name(), since),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Total
        long total = verdictCounts.values().stream().mapToLong(Long::longValue).sum();

        // Deployment tier breakdown for actionable tiers
        var actionable = verdictRepository.findByDeploymentTierInAndCreatedAtAfterOrderByCreatedAtDesc(
                Arrays.stream(DeploymentTier.values())
                        .filter(DeploymentTier::isActionable)
                        .map(Enum::name)
                        .toList(),
                since
        );
        Map<String, Long> tierCounts = actionable.stream()
                .collect(Collectors.groupingBy(
                        e -> DeploymentTier.valueOf(e.getDeploymentTier()).displayName(),
                        Collectors.counting()
                ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackHours", lookbackHours);
        result.put("total", total);
        result.put("byVerdict", verdictCounts);
        result.put("byDeploymentTier", tierCounts);
        result.put("generatedAt", Instant.now().toString());
        return result;
    }
}
