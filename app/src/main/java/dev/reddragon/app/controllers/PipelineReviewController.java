package dev.reddragon.app.controllers;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.PipelineReviewRequest;
import dev.reddragon.app.models.PipelineRunResult;
import dev.reddragon.app.services.pipeline.CandidatePipelineOrchestrator;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.services.ManualCandidateIngestionService;
import dev.reddragon.ingestion.services.sec.SecIngestionService;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineReviewController {

    private final ManualCandidateIngestionService manualIngestionService;
    private final SecIngestionService secIngestionService;
    private final MarketDataProvider marketDataProvider;
    private final CandidatePipelineOrchestrator orchestrator;

    /**
     * Run a manually-supplied candidate through the full pipeline.
     * Include {@code "profile": "CONSERVATIVE"} (or AGGRESSIVE / CONCENTRATION_REVIEW)
     * in the request body to override the default STANDARD thresholds.
     */
    @PostMapping("/manual")
    public PipelineRunResult reviewManual(@Valid @RequestBody PipelineReviewRequest request) {
        TradeCandidate candidate = manualIngestionService.process(
                request.getSymbol(),
                request.getCompanyName(),
                request.getCatalystType(),
                request.getHeadline(),
                request.getSummary(),
                request.getStructuralRealityScore(),
                request.getMaterialSignificanceScore(),
                request.getEarlynessScore(),
                request.getReflexivityPotentialScore()
        );
        ValidationProfile profile = request.getProfile() != null
                ? request.getProfile()
                : ValidationProfile.STANDARD;
        return orchestrator.process(candidate, marketBars(candidate.symbol(), request), profile);
    }

    /**
     * Resolve a ticker to SEC CIK, fetch filings, and run each through the pipeline.
     * Use {@code ?profile=CONSERVATIVE} to apply stricter thresholds.
     */
    @GetMapping("/sec/ticker/{ticker}")
    public List<PipelineRunResult> reviewSecCandidatesByTicker(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "30") int lookbackDays,
            @RequestParam(defaultValue = "STANDARD") ValidationProfile profile
    ) {
        List<TradeCandidate> candidates = secIngestionService.processByTicker(ticker);
        return candidates.stream()
                .map(candidate -> orchestrator.process(
                        candidate,
                        providerBars(candidate.symbol(), lookbackDays),
                        profile
                ))
                .toList();
    }

    /**
     * Fetch SEC filings for a CIK and run each through the pipeline.
     * Use {@code ?profile=CONSERVATIVE} to apply stricter thresholds.
     */
    @GetMapping("/sec/{cik}")
    public List<PipelineRunResult> reviewSecCandidates(
            @PathVariable String cik,
            @RequestParam(defaultValue = "30") int lookbackDays,
            @RequestParam(defaultValue = "STANDARD") ValidationProfile profile
    ) {
        List<TradeCandidate> candidates = secIngestionService.process(cik);
        return candidates.stream()
                .map(candidate -> orchestrator.process(
                        candidate,
                        providerBars(candidate.symbol(), lookbackDays),
                        profile
                ))
                .toList();
    }

    private List<MarketBar> marketBars(String symbol, PipelineReviewRequest request) {
        if (request.getBars() != null && !request.getBars().isEmpty()) {
            return request.getBars().stream()
                    .map(bar -> new MarketBar(
                            symbol,
                            bar.getDate(),
                            bar.getOpen(),
                            bar.getHigh(),
                            bar.getLow(),
                            bar.getClose(),
                            bar.getVolume()
                    ))
                    .toList();
        }

        if (request.getMarketDataFrom() != null && request.getMarketDataTo() != null) {
            return marketDataProvider.historicalDailyBars(
                    symbol, request.getMarketDataFrom(), request.getMarketDataTo());
        }

        return List.of();
    }

    private List<MarketBar> providerBars(String symbol, int lookbackDays) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(2, lookbackDays));
        return marketDataProvider.historicalDailyBars(symbol, from, to);
    }
}
