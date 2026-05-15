package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.reddragon.app.models.BacktestBarRequest;
import dev.reddragon.app.models.BacktestFrameRequest;
import dev.reddragon.app.models.BacktestRequest;
import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import dev.reddragon.backtest.models.BacktestFrame;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.backtest.services.BacktestReplayEngine;
import dev.reddragon.ingestion.models.CandidateCatalystType;
import dev.reddragon.ingestion.services.ManualCandidateIngestionService;
import dev.reddragon.marketdata.models.MarketBar;
import dev.reddragon.persistence.domains.BacktestResultEntity;
import dev.reddragon.persistence.services.repositories.BacktestResultRepository;
import lombok.RequiredArgsConstructor;

/**
 * Runs a deterministic backtest against a list of historical candidate frames
 * and persists the per-frame outcomes for later retrieval.
 *
 * <pre>
 * POST /api/backtest                    — run a backtest, returns report + runId
 * GET  /api/backtest/results/{runId}    — retrieve a previously saved run
 * </pre>
 *
 * Each frame supplies one trade candidate (with analyst scores as they were
 * at the time) plus the historical daily bars available at that date.
 */
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestReplayEngine backtestReplayEngine;
    private final ManualCandidateIngestionService ingestionService;
    private final BacktestResultRepository backtestResultRepository;
    private final CalibrationOutcomeService calibrationOutcomeService;

    @PostMapping
    public BacktestReport runBacktest(@RequestBody BacktestRequest request) {
        String strategyName = request.getStrategyName() == null ? "unnamed" : request.getStrategyName().trim();
        String runId = UUID.randomUUID().toString();

        List<BacktestFrame> frames = (request.getFrames() == null ? List.<BacktestFrameRequest>of() : request.getFrames())
                .stream()
                .map(this::toFrame)
                .toList();

        BacktestReport report = backtestReplayEngine.process(strategyName, frames);

        // Persist per-frame outcomes to backtest_result
        Instant now = Instant.now();
        List<BacktestResultEntity> entities = report.outcomes().stream()
                .map(outcome -> toResultEntity(runId, strategyName, outcome, now))
                .toList();
        backtestResultRepository.saveAll(entities);
        calibrationOutcomeService.appendBacktestOutcomes(report.outcomes());

        return report;
    }

    /**
     * Retrieve all frame outcomes for a previously run backtest by its run ID.
     * Returns 404 if no results are found for the given run ID.
     */
    @GetMapping("/results/{runId}")
    public ResponseEntity<List<BacktestResultEntity>> getResults(@PathVariable String runId) {
        List<BacktestResultEntity> results = backtestResultRepository.findByRunIdOrderByScoreDesc(runId);
        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(results);
    }

    /**
     * List all distinct run IDs available in the store, most recent first.
     * Useful for building a run-browser UI.
     */
    @GetMapping("/runs")
    public List<String> listRuns() {
        return backtestResultRepository.findDistinctRunIds();
    }

    private BacktestFrame toFrame(BacktestFrameRequest req) {
        CandidateCatalystType catalystType = parseCatalystType(req.getCatalystType());

        var candidate = ingestionService.process(
                req.getSymbol(),
                req.getCompanyName(),
                catalystType,
                req.getHeadline(),
                req.getSummary(),
                req.getStructuralRealityScore(),
                req.getMaterialSignificanceScore(),
                req.getEarlynessScore(),
                req.getReflexivityPotentialScore()
        );

        List<MarketBar> bars = (req.getBars() == null ? List.<BacktestBarRequest>of() : req.getBars())
                .stream()
                .map(b -> new MarketBar(
                        req.getSymbol(),
                        b.getDate(),
                        b.getOpen(),
                        b.getHigh(),
                        b.getLow(),
                        b.getClose(),
                        b.getVolume()
                ))
                .toList();

        return new BacktestFrame(candidate, bars);
    }

    private BacktestResultEntity toResultEntity(
            String runId,
            String strategyName,
            BacktestOutcome outcome,
            Instant testedAt
    ) {
        return new BacktestResultEntity(
                null,
                runId,
                strategyName,
                outcome.candidate().symbol(),
                outcome.candidate().candidateId(),
                outcome.validation().verdict().name(),
                outcome.validation().score(),
                testedAt
        );
    }

    private CandidateCatalystType parseCatalystType(String raw) {
        if (raw == null || raw.isBlank()) {
            return CandidateCatalystType.MANUAL_THESIS;
        }
        try {
            return CandidateCatalystType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CandidateCatalystType.MANUAL_THESIS;
        }
    }
}
