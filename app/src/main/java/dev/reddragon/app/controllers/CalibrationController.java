package dev.reddragon.app.controllers;

import dev.reddragon.app.models.CalibrationBooleanView;
import dev.reddragon.app.models.CalibrationCountView;
import dev.reddragon.app.models.CalibrationMetricView;
import dev.reddragon.app.models.CalibrationOutcomeSampleRequest;
import dev.reddragon.app.models.CalibrationOutcomeView;
import dev.reddragon.app.models.CalibrationSummaryView;
import dev.reddragon.app.models.CalibrationTimestampView;
import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import dev.reddragon.domain.models.AnalyticsScoreBreakdown;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.OutcomeSample;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Evaluates long-horizon framework drift and calibration quality from trader-supplied outcomes.
 *
 * <p>Usage: after a batch of trades has closed, POST the outcomes here to learn whether
 * the disequilibrium validation framework is drifting from its historical performance
 * assumptions. The response tells you whether to tighten, loosen, or review specific thresholds.
 *
 * <pre>
 * POST /api/calibration
 * [
 *   {
 *     "candidateId": "...", "symbol": "ASTS",
 *     "realizedReturn": 0.18, "maxDrawdown": 0.05, "daysHeld": 12,
 *     "thesisWorked": true,
 *     "structuralRealityScore": 0.88, "materialSignificanceScore": 0.82,
 *     "earlynessScore": 0.79, "equilibriumQualityScore": 0.74,
 *     "reflexivityPotentialScore": 0.70, "asymmetryScore": 0.85,
 *     "regimeCompatibilityScore": 0.65, "deploymentConfidenceScore": 0.80
 *   },
 *   ...
 * ]
 * </pre>
 */
@RestController
@RequestMapping("/api/calibration")
@RequiredArgsConstructor
public class CalibrationController {

    private final CalibrationOutcomeService calibrationOutcomeService;

    @GetMapping
    public CalibrationReport current() {
        return calibrationOutcomeService.currentReport();
    }

    @GetMapping("/outcomes")
    public List<CalibrationOutcomeView> recentOutcomes(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.recentOutcomes(limit).stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/summary")
    public CalibrationSummaryView summary(@RequestParam(defaultValue = "100") int limit) {
        var summary = calibrationOutcomeService.summary(limit);
        return new CalibrationSummaryView(
                summary.sampleSize(),
                summary.winRate(),
                summary.averageReturn(),
                summary.averageDrawdown()
        );
    }

    @GetMapping("/summary/win-rate")
    public CalibrationMetricView winRateSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("winRate", calibrationOutcomeService.winRate(limit));
    }

    @GetMapping("/summary/min-drawdown")
    public CalibrationMetricView minDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("minDrawdown", calibrationOutcomeService.minDrawdownValue(limit));
    }

    @GetMapping("/summary/max-drawdown")
    public CalibrationMetricView maxDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("maxDrawdown", calibrationOutcomeService.maxDrawdownValue(limit));
    }

    @GetMapping("/summary/min-return")
    public CalibrationMetricView minReturnSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("minReturn", calibrationOutcomeService.minReturn(limit));
    }

    @GetMapping("/summary/max-return")
    public CalibrationMetricView maxReturnSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("maxReturn", calibrationOutcomeService.maxReturn(limit));
    }

    @GetMapping("/summary/holding-days")
    public CalibrationMetricView averageHoldingDays(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("averageHoldingDays", calibrationOutcomeService.averageDaysHeld(limit));
    }

    @GetMapping("/summary/median")
    public CalibrationMetricView medianSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("medianReturn", calibrationOutcomeService.medianReturn(limit));
    }

    @GetMapping("/summary/median-drawdown")
    public CalibrationMetricView medianDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        return new CalibrationMetricView("medianDrawdown", calibrationOutcomeService.medianDrawdown(limit));
    }

    @GetMapping("/summary/{symbol}/win-rate")
    public CalibrationMetricView winRateBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView("winRate", calibrationOutcomeService.winRateForSymbol(symbol, limit));
    }

    @GetMapping("/summary/{symbol}/max-drawdown")
    public CalibrationMetricView maxDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView("maxDrawdown", calibrationOutcomeService.maxDrawdownForSymbol(symbol, limit));
    }

    @GetMapping("/summary/{symbol}/min-drawdown")
    public CalibrationMetricView minDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView("minDrawdown", calibrationOutcomeService.minDrawdownForSymbol(symbol, limit));
    }

    @GetMapping("/summary/{symbol}/average-drawdown")
    public CalibrationMetricView averageDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView(
                "averageDrawdown",
                calibrationOutcomeService.averageDrawdownForSymbol(symbol, limit)
        );
    }

    @GetMapping("/summary/{symbol}/average-return")
    public CalibrationMetricView averageReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView(
                "averageReturn",
                calibrationOutcomeService.averageReturnForSymbol(symbol, limit)
        );
    }

    @GetMapping("/summary/{symbol}/min-return")
    public CalibrationMetricView minReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView("minReturn", calibrationOutcomeService.minReturnForSymbol(symbol, limit));
    }

    @GetMapping("/summary/{symbol}/max-return")
    public CalibrationMetricView maxReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView("maxReturn", calibrationOutcomeService.maxReturnForSymbol(symbol, limit));
    }

    @GetMapping("/summary/{symbol}/median-return")
    public CalibrationMetricView medianReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView(
                "medianReturn",
                calibrationOutcomeService.medianReturnForSymbol(symbol, limit)
        );
    }

    @GetMapping("/summary/{symbol}/holding-days")
    public CalibrationMetricView averageHoldingDaysBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView(
                "averageHoldingDays",
                calibrationOutcomeService.averageDaysHeldForSymbol(symbol, limit)
        );
    }

    @GetMapping("/summary/{symbol}/median-drawdown")
    public CalibrationMetricView medianDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return new CalibrationMetricView(
                "medianDrawdown",
                calibrationOutcomeService.medianDrawdownForSymbol(symbol, limit)
        );
    }

    @GetMapping("/summary/{symbol}")
    public CalibrationSummaryView summaryBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        var summary = calibrationOutcomeService.summaryForSymbol(symbol, limit);
        return new CalibrationSummaryView(
                summary.sampleSize(),
                summary.winRate(),
                summary.averageReturn(),
                summary.averageDrawdown()
        );
    }

    @GetMapping(value = "/outcomes/export", produces = "text/csv")
    public ResponseEntity<String> exportOutcomesCsv(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(calibrationOutcomeService.exportRecentOutcomesCsv(limit));
    }

    @GetMapping("/outcomes/symbols")
    public List<String> outcomeSymbols() {
        return calibrationOutcomeService.symbols();
    }

    @GetMapping("/outcomes/high-drawdown")
    public List<CalibrationOutcomeView> highDrawdownOutcomes(
            @RequestParam(defaultValue = "0.15") double minDrawdown,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return calibrationOutcomeService.highDrawdownOutcomes(minDrawdown, limit).stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/outcomes/worst")
    public List<CalibrationOutcomeView> worstOutcomes(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.worstOutcomesByReturn(limit).stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/outcomes/top")
    public List<CalibrationOutcomeView> topOutcomes(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.topOutcomesByReturn(limit).stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/outcomes/page")
    public List<CalibrationOutcomeView> outcomesPage(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return calibrationOutcomeService.recentOutcomesPage(offset, limit).stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/outcomes/last-updated")
    public CalibrationTimestampView latestObservedAt() {
        String observedAt = calibrationOutcomeService.latestObservedAt();
        return new CalibrationTimestampView(
                "latestObservedAt",
                observedAt.isBlank() ? null : Instant.parse(observedAt)
        );
    }

    @GetMapping("/outcomes/count")
    public CalibrationCountView countOutcomes() {
        return new CalibrationCountView("outcomeCount", calibrationOutcomeService.countOutcomes());
    }

    @GetMapping("/outcomes/exists/{symbol}")
    public CalibrationBooleanView outcomeExistsBySymbol(@PathVariable String symbol) {
        return new CalibrationBooleanView("hasOutcomes", calibrationOutcomeService.hasOutcomesForSymbol(symbol));
    }

    @GetMapping("/outcomes/count/{symbol}")
    public CalibrationCountView countOutcomesBySymbol(@PathVariable String symbol) {
        return new CalibrationCountView("outcomeCount", calibrationOutcomeService.countOutcomesForSymbol(symbol));
    }

    @GetMapping(value = "/outcomes/{symbol}/export", produces = "text/csv")
    public ResponseEntity<String> exportOutcomesCsvBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(calibrationOutcomeService.exportRecentOutcomesCsvForSymbol(symbol, limit));
    }

    @DeleteMapping("/outcomes")
    public CalibrationCountView deleteAllOutcomes() {
        return new CalibrationCountView("deletedCount", calibrationOutcomeService.clearAllOutcomes());
    }

    @DeleteMapping("/outcomes/{symbol}")
    public CalibrationCountView deleteBySymbol(@PathVariable String symbol) {
        return new CalibrationCountView("deletedCount", calibrationOutcomeService.deleteOutcomesForSymbol(symbol));
    }

    @GetMapping("/outcomes/{symbol}")
    public List<CalibrationOutcomeView> recentOutcomesBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return calibrationOutcomeService.recentOutcomesForSymbol(symbol, limit).stream()
                .map(this::toView)
                .toList();
    }

    @PostMapping
    public CalibrationReport analyze(@Valid @RequestBody List<@Valid CalibrationOutcomeSampleRequest> samples) {
        List<OutcomeSample> outcomeSamples = (samples == null ? List.<CalibrationOutcomeSampleRequest>of() : samples)
                .stream()
                .map(this::toOutcomeSample)
                .toList();

        return calibrationOutcomeService.analyzeAndAppend(outcomeSamples);
    }

    private CalibrationOutcomeView toView(OutcomeSample sample) {
        return new CalibrationOutcomeView(
                sample.candidateId(),
                sample.symbol(),
                sample.observedAt(),
                sample.realizedReturn(),
                sample.maxDrawdown(),
                sample.daysHeld(),
                sample.thesisWorked()
        );
    }

    private OutcomeSample toOutcomeSample(CalibrationOutcomeSampleRequest req) {
        String candidateId = req.getCandidateId() == null || req.getCandidateId().isBlank()
                ? "unknown"
                : req.getCandidateId();
        String symbol = req.getSymbol().toUpperCase();

        AnalyticsScoreBreakdown breakdown = new AnalyticsScoreBreakdown(
                req.getStructuralRealityScore(),
                req.getMaterialSignificanceScore(),
                req.getEarlynessScore(),
                req.getEquilibriumQualityScore(),
                req.getReflexivityPotentialScore(),
                req.getAsymmetryScore(),
                req.getRegimeCompatibilityScore(),
                req.getDeploymentConfidenceScore()
        );

        return new OutcomeSample(
                candidateId,
                symbol,
                Instant.now(),
                breakdown,
                req.getRealizedReturn(),
                req.getMaxDrawdown(),
                req.getDaysHeld(),
                req.isThesisWorked()
        );
    }
}
