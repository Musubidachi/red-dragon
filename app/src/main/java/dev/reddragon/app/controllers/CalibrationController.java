package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;

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

import dev.reddragon.domain.models.AnalyticsScoreBreakdown;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.OutcomeSample;
import dev.reddragon.app.models.CalibrationOutcomeSampleRequest;
import dev.reddragon.app.models.CalibrationOutcomeView;
import dev.reddragon.app.models.CalibrationSummaryView;
import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import lombok.RequiredArgsConstructor;

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

    @GetMapping
    public CalibrationReport current() {
        return calibrationOutcomeService.currentReport();
    }


    private final CalibrationOutcomeService calibrationOutcomeService;


    @GetMapping("/outcomes")
    public List<CalibrationOutcomeView> recentOutcomes(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.recentOutcomes(limit).stream()
                .map(s -> new CalibrationOutcomeView(
                        s.candidateId(),
                        s.symbol(),
                        s.observedAt(),
                        s.realizedReturn(),
                        s.maxDrawdown(),
                        s.daysHeld(),
                        s.thesisWorked()
                ))
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
    public CalibrationSummaryView winRateSummary(@RequestParam(defaultValue = "100") int limit) {
        double winRate = calibrationOutcomeService.winRate(limit);
        return new CalibrationSummaryView(0, winRate, 0.0, 0.0);
    }

    @GetMapping("/summary/min-drawdown")
    public CalibrationSummaryView minDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        double minDrawdown = calibrationOutcomeService.minDrawdownValue(limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, minDrawdown);
    }

    @GetMapping("/summary/max-drawdown")
    public CalibrationSummaryView maxDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        double maxDrawdown = calibrationOutcomeService.maxDrawdownValue(limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, maxDrawdown);
    }

    @GetMapping("/summary/min-return")
    public CalibrationSummaryView minReturnSummary(@RequestParam(defaultValue = "100") int limit) {
        double min = calibrationOutcomeService.minReturn(limit);
        return new CalibrationSummaryView(0, 0.0, min, 0.0);
    }

    @GetMapping("/summary/max-return")
    public CalibrationSummaryView maxReturnSummary(@RequestParam(defaultValue = "100") int limit) {
        double max = calibrationOutcomeService.maxReturn(limit);
        return new CalibrationSummaryView(0, 0.0, max, 0.0);
    }

    @GetMapping("/summary/holding-days")
    public CalibrationSummaryView averageHoldingDays(@RequestParam(defaultValue = "100") int limit) {
        double average = calibrationOutcomeService.averageDaysHeld(limit);
        return new CalibrationSummaryView(0, 0.0, average, 0.0);
    }

    @GetMapping("/summary/median")
    public CalibrationSummaryView medianSummary(@RequestParam(defaultValue = "100") int limit) {
        double median = calibrationOutcomeService.medianReturn(limit);
        return new CalibrationSummaryView(0, 0.0, median, 0.0);
    }

    @GetMapping("/summary/median-drawdown")
    public CalibrationSummaryView medianDrawdownSummary(@RequestParam(defaultValue = "100") int limit) {
        double medianDrawdown = calibrationOutcomeService.medianDrawdown(limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, medianDrawdown);
    }



    @GetMapping("/summary/{symbol}/win-rate")
    public CalibrationSummaryView winRateBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double winRate = calibrationOutcomeService.winRateForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, winRate, 0.0, 0.0);
    }



    @GetMapping("/summary/{symbol}/max-drawdown")
    public CalibrationSummaryView maxDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double max = calibrationOutcomeService.maxDrawdownForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, max);
    }

    @GetMapping("/summary/{symbol}/min-drawdown")
    public CalibrationSummaryView minDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double min = calibrationOutcomeService.minDrawdownForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, min);
    }

    @GetMapping("/summary/{symbol}/average-drawdown")
    public CalibrationSummaryView averageDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double avg = calibrationOutcomeService.averageDrawdownForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, avg);
    }

    @GetMapping("/summary/{symbol}/average-return")
    public CalibrationSummaryView averageReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double avg = calibrationOutcomeService.averageReturnForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, avg, 0.0);
    }

    @GetMapping("/summary/{symbol}/min-return")
    public CalibrationSummaryView minReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double min = calibrationOutcomeService.minReturnForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, min, 0.0);
    }

    @GetMapping("/summary/{symbol}/max-return")
    public CalibrationSummaryView maxReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double max = calibrationOutcomeService.maxReturnForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, max, 0.0);
    }

    @GetMapping("/summary/{symbol}/median-return")
    public CalibrationSummaryView medianReturnBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double median = calibrationOutcomeService.medianReturnForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, median, 0.0);
    }

    @GetMapping("/summary/{symbol}/holding-days")
    public CalibrationSummaryView averageHoldingDaysBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double averageDaysHeld = calibrationOutcomeService.averageDaysHeldForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, averageDaysHeld, 0.0);
    }

    @GetMapping("/summary/{symbol}/median-drawdown")
    public CalibrationSummaryView medianDrawdownBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit
    ) {
        double medianDrawdown = calibrationOutcomeService.medianDrawdownForSymbol(symbol, limit);
        return new CalibrationSummaryView(0, 0.0, 0.0, medianDrawdown);
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
                .map(s -> new CalibrationOutcomeView(
                        s.candidateId(),
                        s.symbol(),
                        s.observedAt(),
                        s.realizedReturn(),
                        s.maxDrawdown(),
                        s.daysHeld(),
                        s.thesisWorked()
                ))
                .toList();
    }

    @GetMapping("/outcomes/worst")
    public List<CalibrationOutcomeView> worstOutcomes(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.worstOutcomesByReturn(limit).stream()
                .map(s -> new CalibrationOutcomeView(
                        s.candidateId(),
                        s.symbol(),
                        s.observedAt(),
                        s.realizedReturn(),
                        s.maxDrawdown(),
                        s.daysHeld(),
                        s.thesisWorked()
                ))
                .toList();
    }

    @GetMapping("/outcomes/top")
    public List<CalibrationOutcomeView> topOutcomes(@RequestParam(defaultValue = "25") int limit) {
        return calibrationOutcomeService.topOutcomesByReturn(limit).stream()
                .map(s -> new CalibrationOutcomeView(
                        s.candidateId(),
                        s.symbol(),
                        s.observedAt(),
                        s.realizedReturn(),
                        s.maxDrawdown(),
                        s.daysHeld(),
                        s.thesisWorked()
                ))
                .toList();
    }

    @GetMapping("/outcomes/page")
    public List<CalibrationOutcomeView> outcomesPage(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return calibrationOutcomeService.recentOutcomesPage(offset, limit).stream()
                .map(s -> new CalibrationOutcomeView(
                        s.candidateId(),
                        s.symbol(),
                        s.observedAt(),
                        s.realizedReturn(),
                        s.maxDrawdown(),
                        s.daysHeld(),
                        s.thesisWorked()
                ))
                .toList();
    }

    @GetMapping("/outcomes/last-updated")
    public CalibrationOutcomeView latestObservedAt() {
        String observedAt = calibrationOutcomeService.latestObservedAt();
        return new CalibrationOutcomeView("", "", observedAt.isBlank() ? null : java.time.Instant.parse(observedAt), 0.0, 0.0, 0, false);
    }

    @GetMapping("/outcomes/count")
    public CalibrationSummaryView countOutcomes() {
        long count = calibrationOutcomeService.countOutcomes();
        return new CalibrationSummaryView((int) count, 0.0, 0.0, 0.0);
    }


    @GetMapping("/outcomes/exists/{symbol}")
    public CalibrationSummaryView outcomeExistsBySymbol(@PathVariable String symbol) {
        boolean exists = calibrationOutcomeService.hasOutcomesForSymbol(symbol);
        return new CalibrationSummaryView(exists ? 1 : 0, 0.0, 0.0, 0.0);
    }

    @GetMapping("/outcomes/count/{symbol}")
    public CalibrationSummaryView countOutcomesBySymbol(@PathVariable String symbol) {
        long count = calibrationOutcomeService.countOutcomesForSymbol(symbol);
        return new CalibrationSummaryView((int) count, 0.0, 0.0, 0.0);
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
    public CalibrationSummaryView deleteAllOutcomes() {
        long deleted = calibrationOutcomeService.clearAllOutcomes();
        return new CalibrationSummaryView((int) deleted, 0.0, 0.0, 0.0);
    }

    @DeleteMapping("/outcomes/{symbol}")
    public CalibrationSummaryView deleteBySymbol(@PathVariable String symbol) {
        long deleted = calibrationOutcomeService.deleteOutcomesForSymbol(symbol);
        return new CalibrationSummaryView((int) deleted, 0.0, 0.0, 0.0);
    }

    @GetMapping("/outcomes/{symbol}")
    public List<CalibrationOutcomeView> recentOutcomesBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return calibrationOutcomeService.recentOutcomesForSymbol(symbol, limit).stream()
                .map(s -> new CalibrationOutcomeView(
                        s.candidateId(),
                        s.symbol(),
                        s.observedAt(),
                        s.realizedReturn(),
                        s.maxDrawdown(),
                        s.daysHeld(),
                        s.thesisWorked()
                ))
                .toList();
    }

    @PostMapping
    public CalibrationReport analyze(@RequestBody List<CalibrationOutcomeSampleRequest> samples) {
        List<OutcomeSample> outcomeSamples = (samples == null ? List.<CalibrationOutcomeSampleRequest>of() : samples)
                .stream()
                .map(this::toOutcomeSample)
                .toList();

        return calibrationOutcomeService.analyzeAndAppend(outcomeSamples);
    }

    private OutcomeSample toOutcomeSample(CalibrationOutcomeSampleRequest req) {
        String candidateId = req.getCandidateId() == null || req.getCandidateId().isBlank()
                ? "unknown"
                : req.getCandidateId();
        String symbol = req.getSymbol() == null || req.getSymbol().isBlank()
                ? "UNKNOWN"
                : req.getSymbol().toUpperCase();

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
