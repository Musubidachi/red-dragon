package dev.reddragon.app.api;

import dev.reddragon.persistence.entity.ValidationVerdictEntity;
import dev.reddragon.persistence.repository.ValidationVerdictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regime-aware opportunity quality snapshot.
 *
 * <pre>
 * GET /api/stats/opportunity-quality?lookbackDays=30&topSymbols=10
 * </pre>
 */
@RestController
@RequestMapping("/api/stats/opportunity-quality")
@RequiredArgsConstructor
public class OpportunityQualityController {

    private final ValidationVerdictRepository verdictRepository;

    @GetMapping
    public Map<String, Object> summary(
            @RequestParam(defaultValue = "30") int lookbackDays,
            @RequestParam(defaultValue = "10") int topSymbols
    ) {
        int safeLookbackDays = Math.min(Math.max(1, lookbackDays), 365);
        int safeTopSymbols = Math.min(Math.max(1, topSymbols), 50);

        Instant since = Instant.now().minus(safeLookbackDays, ChronoUnit.DAYS);
        List<ValidationVerdictEntity> verdicts = verdictRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);

        long highConvictionCount = verdicts.stream()
                .filter(v -> v.getScore() >= 0.75d)
                .count();
        long mediumConvictionCount = verdicts.stream()
                .filter(v -> v.getScore() >= 0.55d && v.getScore() < 0.75d)
                .count();
        long lowConvictionCount = verdicts.stream()
                .filter(v -> v.getScore() < 0.55d)
                .count();

        Map<String, Long> verdictMix = verdicts.stream()
                .collect(Collectors.groupingBy(ValidationVerdictEntity::getVerdict, LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> topSymbolsByAverageScore = verdicts.stream()
                .collect(Collectors.groupingBy(ValidationVerdictEntity::getSymbol))
                .entrySet().stream()
                .map(entry -> {
                    String symbol = entry.getKey();
                    List<ValidationVerdictEntity> symbolVerdicts = entry.getValue();
                    double avgScore = symbolVerdicts.stream()
                            .mapToDouble(ValidationVerdictEntity::getScore)
                            .average()
                            .orElse(0.0d);
                    long sampleSize = symbolVerdicts.size();
                    return Map.of(
                            "symbol", symbol,
                            "avgScore", avgScore,
                            "sampleSize", sampleSize
                    );
                })
                .sorted(Comparator.comparingDouble((Map<String, Object> row) -> (double) row.get("avgScore")).reversed()
                        .thenComparing((Map<String, Object> row) -> (long) row.get("sampleSize"), Comparator.reverseOrder()))
                .limit(safeTopSymbols)
                .toList();

        Map<String, Long> deploymentTierMix = verdicts.stream()
                .collect(Collectors.groupingBy(ValidationVerdictEntity::getDeploymentTier, LinkedHashMap::new, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lookbackDays", safeLookbackDays);
        result.put("sampleSize", verdicts.size());
        result.put("convictionBands", Map.of(
                "high", highConvictionCount,
                "medium", mediumConvictionCount,
                "low", lowConvictionCount
        ));
        result.put("verdictMix", sortMapByValueDesc(verdictMix));
        result.put("deploymentTierMix", sortMapByValueDesc(deploymentTierMix));
        result.put("topSymbolsByAverageScore", topSymbolsByAverageScore);
        result.put("generatedAt", Instant.now().toString());
        return result;
    }

    private Map<String, Long> sortMapByValueDesc(Map<String, Long> source) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
