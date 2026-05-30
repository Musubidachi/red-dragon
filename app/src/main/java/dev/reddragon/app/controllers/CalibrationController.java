package dev.reddragon.app.controllers;

import dev.reddragon.app.models.CalibrationBooleanView;
import dev.reddragon.app.models.CalibrationCountView;
import dev.reddragon.app.models.CalibrationMetricView;
import dev.reddragon.app.models.CalibrationOutcomeSampleRequest;
import dev.reddragon.app.models.CalibrationOutcomeView;
import dev.reddragon.app.models.CalibrationSummaryDetailView;
import dev.reddragon.app.models.CalibrationSummaryView;
import dev.reddragon.app.models.CalibrationTimestampView;
import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import dev.reddragon.domain.models.AnalyticsScoreBreakdown;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.OutcomeSample;
import dev.reddragon.persistence.domains.CalibrationReportEntity;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final String DEPRECATION_HEADER = "X-Red-Dragon-Deprecated-Endpoint";
    private static final String SUMMARY_DETAILS_PATH = "/api/calibration/summary/details";

    private final CalibrationOutcomeService calibrationOutcomeService;

    @GetMapping
    public CalibrationReport current() {
        return calibrationOutcomeService.currentReport();
    }

    @GetMapping("/reports")
    public List<CalibrationReportEntity> recentReports(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.recentReports(limit);
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

    @GetMapping("/summary/batch")
    public Map<String, CalibrationSummaryView> summaryBatch(
            @RequestParam String symbols,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return Arrays.stream(symbols.split(","))
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .filter(symbol -> !symbol.isBlank())
                .distinct()
                .limit(100)
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> {
                            var summary = calibrationOutcomeService.summaryForSymbol(symbol, limit);
                            return new CalibrationSummaryView(
                                    summary.sampleSize(),
                                    summary.winRate(),
                                    summary.averageReturn(),
                                    summary.averageDrawdown()
                            );
                        }
                ));
    }

    @GetMapping("/summary/details")
    public CalibrationSummaryDetailView summaryDetails(@RequestParam(defaultValue = "100") int limit) {
        var summary = calibrationOutcomeService.summary(limit);
        return new CalibrationSummaryDetailView(
                summary.sampleSize(),
                summary.winRate(),
                summary.averageReturn(),
                summary.averageDrawdown(),
                calibrationOutcomeService.minReturn(limit),
                calibrationOutcomeService.maxReturn(limit),
                calibrationOutcomeService.medianReturn(limit),
                calibrationOutcomeService.minDrawdownValue(limit),
                calibrationOutcomeService.maxDrawdownValue(limit),
                calibrationOutcomeService.medianDrawdown(limit),
                calibrationOutcomeService.averageDaysHeld(limit)
        );
    }

    @GetMapping("/summary/win-rate")
    public ResponseEntity<CalibrationMetricView> winRateSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("winRate", calibrationOutcomeService.winRate(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/min-drawdown")
    public ResponseEntity<CalibrationMetricView> minDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("minDrawdown", calibrationOutcomeService.minDrawdownValue(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/max-drawdown")
    public ResponseEntity<CalibrationMetricView> maxDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("maxDrawdown", calibrationOutcomeService.maxDrawdownValue(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/min-return")
    public ResponseEntity<CalibrationMetricView> minReturnSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("minReturn", calibrationOutcomeService.minReturn(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/max-return")
    public ResponseEntity<CalibrationMetricView> maxReturnSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("maxReturn", calibrationOutcomeService.maxReturn(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/holding-days")
    public ResponseEntity<CalibrationMetricView> averageHoldingDays(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("averageHoldingDays", calibrationOutcomeService.averageDaysHeld(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/median")
    public ResponseEntity<CalibrationMetricView> medianSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("medianReturn", calibrationOutcomeService.medianReturn(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/median-drawdown")
    public ResponseEntity<CalibrationMetricView> medianDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        return deprecatedMetric("medianDrawdown", calibrationOutcomeService.medianDrawdown(limit), SUMMARY_DETAILS_PATH);
    }

    @GetMapping("/summary/{symbol}/win-rate")
    public ResponseEntity<CalibrationMetricView> winRateBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "winRate",
                calibrationOutcomeService.winRateForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/max-drawdown")
    public ResponseEntity<CalibrationMetricView> maxDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "maxDrawdown",
                calibrationOutcomeService.maxDrawdownForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/min-drawdown")
    public ResponseEntity<CalibrationMetricView> minDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "minDrawdown",
                calibrationOutcomeService.minDrawdownForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/average-drawdown")
    public ResponseEntity<CalibrationMetricView> averageDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "averageDrawdown",
                calibrationOutcomeService.averageDrawdownForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/average-return")
    public ResponseEntity<CalibrationMetricView> averageReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "averageReturn",
                calibrationOutcomeService.averageReturnForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/min-return")
    public ResponseEntity<CalibrationMetricView> minReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "minReturn",
                calibrationOutcomeService.minReturnForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/max-return")
    public ResponseEntity<CalibrationMetricView> maxReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "maxReturn",
                calibrationOutcomeService.maxReturnForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/median-return")
    public ResponseEntity<CalibrationMetricView> medianReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "medianReturn",
                calibrationOutcomeService.medianReturnForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/holding-days")
    public ResponseEntity<CalibrationMetricView> averageHoldingDaysBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "averageHoldingDays",
                calibrationOutcomeService.averageDaysHeldForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
        );
    }

    @GetMapping("/summary/{symbol}/median-drawdown")
    public ResponseEntity<CalibrationMetricView> medianDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return deprecatedMetric(
                "medianDrawdown",
                calibrationOutcomeService.medianDrawdownForSymbol(symbol, limit),
                "/api/calibration/summary/" + symbol + "/details"
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

    @GetMapping("/summary/{symbol}/details")
    public CalibrationSummaryDetailView summaryDetailsBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        var summary = calibrationOutcomeService.summaryForSymbol(symbol, limit);
        return new CalibrationSummaryDetailView(
                summary.sampleSize(),
                summary.winRate(),
                summary.averageReturn(),
                summary.averageDrawdown(),
                calibrationOutcomeService.minReturnForSymbol(symbol, limit),
                calibrationOutcomeService.maxReturnForSymbol(symbol, limit),
                calibrationOutcomeService.medianReturnForSymbol(symbol, limit),
                calibrationOutcomeService.minDrawdownForSymbol(symbol, limit),
                calibrationOutcomeService.maxDrawdownForSymbol(symbol, limit),
                calibrationOutcomeService.medianDrawdownForSymbol(symbol, limit),
                calibrationOutcomeService.averageDaysHeldForSymbol(symbol, limit)
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

    private ResponseEntity<CalibrationMetricView> deprecatedMetric(String metric, double value, String replacementPath) {
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .header(DEPRECATION_HEADER, "Use " + replacementPath + " instead.")
                .body(new CalibrationMetricView(metric, value));
    }
}
