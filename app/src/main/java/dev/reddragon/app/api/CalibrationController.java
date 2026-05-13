package dev.reddragon.app.api;

import dev.reddragon.analytics.model.AnalyticsScoreBreakdown;
import dev.reddragon.analytics.model.CalibrationReport;
import dev.reddragon.analytics.model.OutcomeSample;
import dev.reddragon.analytics.service.LongHorizonCalibrationAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    private final LongHorizonCalibrationAnalyzer calibrationAnalyzer;

    @PostMapping
    public CalibrationReport analyze(@RequestBody List<CalibrationOutcomeSampleRequest> samples) {
        List<OutcomeSample> outcomeSamples = (samples == null ? List.<CalibrationOutcomeSampleRequest>of() : samples)
                .stream()
                .map(this::toOutcomeSample)
                .toList();

        return calibrationAnalyzer.process(outcomeSamples);
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
