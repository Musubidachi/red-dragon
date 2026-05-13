package dev.reddragon.app.api;

import dev.reddragon.app.pipeline.CandidatePipelineOrchestrator;
import dev.reddragon.app.pipeline.PipelineRunResult;
import dev.reddragon.ingestion.model.TradeCandidate;
import dev.reddragon.ingestion.sec.SecIngestionService;
import dev.reddragon.marketdata.model.MarketBar;
import dev.reddragon.marketdata.provider.MarketDataProvider;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Runs multiple CIKs through the SEC ingestion + pipeline in a single request
 * and returns all results sorted by validation score descending.
 *
 * <pre>
 * GET /api/pipeline/sec/watch-list?ciks=0000950170,0001326380&lookbackDays=30&profile=STANDARD
 * </pre>
 *
 * Duplicate candidates (already in the database) are included in the response
 * with {@code duplicate: true} so the caller knows they were skipped.
 */
@RestController
@RequestMapping("/api/pipeline/sec")
@RequiredArgsConstructor
public class SecWatchListController {

    private static final Logger log = LoggerFactory.getLogger(SecWatchListController.class);
    private static final int MAX_CIKS = 20;

    private final SecIngestionService secIngestionService;
    private final MarketDataProvider marketDataProvider;
    private final CandidatePipelineOrchestrator orchestrator;

    @GetMapping("/watch-list")
    public List<PipelineRunResult> runWatchList(
            @RequestParam String ciks,
            @RequestParam(defaultValue = "30") int lookbackDays,
            @RequestParam(defaultValue = "STANDARD") ValidationProfile profile
    ) {
        List<String> cikList = parseCiks(ciks);
        if (cikList.isEmpty()) {
            throw new IllegalArgumentException("At least one CIK is required.");
        }

        List<PipelineRunResult> results = new ArrayList<>();

        for (String cik : cikList) {
            try {
                List<TradeCandidate> candidates = secIngestionService.process(cik);
                for (TradeCandidate candidate : candidates) {
                    List<MarketBar> bars = providerBars(candidate.symbol(), lookbackDays);
                    results.add(orchestrator.process(candidate, bars, profile));
                }
            } catch (Exception e) {
                log.warn("SEC ingestion failed for CIK {}: {}", cik, e.getMessage());
            }
        }

        // Sort: non-duplicates by score desc, duplicates at the bottom
        results.sort(Comparator
                .comparing(PipelineRunResult::duplicate)
                .thenComparing(r -> r.validation() != null ? -r.validation().score() : 0.0)
        );

        return results;
    }

    private List<String> parseCiks(String ciks) {
        return Arrays.stream(ciks.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(MAX_CIKS)
                .toList();
    }

    private List<MarketBar> providerBars(String symbol, int lookbackDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(2, lookbackDays));
        return marketDataProvider.historicalDailyBars(symbol, from, to);
    }
}
