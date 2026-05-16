package dev.reddragon.app.services.pipeline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.reddragon.app.models.PipelineRunResult;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.services.sec.SecIngestionService;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import dev.reddragon.validation.config.ValidationProfile;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecWatchListRunner {
    private final SecIngestionService secIngestionService;
    private final MarketDataProvider marketDataProvider;
    private final CandidatePipelineOrchestrator orchestrator;

    @Timed(value = "reddragon.sec.watchlist", description = "SEC watch-list run duration", histogram = true)
    public List<PipelineRunResult> run(List<String> cikList, int lookbackDays, ValidationProfile profile) {
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
        results.sort(Comparator
                .comparingInt((PipelineRunResult r) -> r.duplicate() ? 1 : 0)
                .thenComparingDouble((PipelineRunResult r) -> r.validation() != null ? -r.validation().score() : 0.0d));
        return results;
    }

    private List<MarketBar> providerBars(String symbol, int lookbackDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(2, lookbackDays));
        return marketDataProvider.historicalDailyBars(symbol, from, to);
    }
}
