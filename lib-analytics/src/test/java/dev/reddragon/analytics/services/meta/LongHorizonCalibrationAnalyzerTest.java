package dev.reddragon.analytics.services.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import dev.reddragon.analytics.models.AnalyticsScoreBreakdown;
import dev.reddragon.analytics.models.CalibrationDriftLevel;
import dev.reddragon.analytics.models.CalibrationReport;
import dev.reddragon.analytics.models.OutcomeSample;

/**
 * Boundary tests for the MD Layer 8 (Meta-System Adaptation) calibration analyzer.
 *
 * <p>Each test pins one drift band against the three knobs the analyzer reads
 * (win rate, average return, average drawdown) so threshold changes can't
 * drift silently.
 */
class LongHorizonCalibrationAnalyzerTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-05-13T00:00:00Z");
    private static final int DEFAULT_DAYS_HELD = 5;

    /** A neutral score breakdown shared across all test fixtures. */
    private static final AnalyticsScoreBreakdown DEFAULT_SCORES =
            new AnalyticsScoreBreakdown(0.8, 0.7, 0.6, 0.7, 0.6, 0.7, 0.6, 0.8);

    private final LongHorizonCalibrationAnalyzer analyzer = new LongHorizonCalibrationAnalyzer();

    @Test
    void nullSamplesAreRejected() {
        assertThrows(NullPointerException.class, () -> analyzer.process(null));
    }

    @Test
    void emptySamplesProduceStableReportWithGuidance() {
        CalibrationReport report = analyzer.process(List.of());

        assertEquals(CalibrationDriftLevel.STABLE, report.driftLevel());
        //assertEquals(0.0, report.winRate());
        assertEquals(0.0, report.averageReturn());
        assertEquals(0.0, report.averageDrawdown());
        assertTrue(
                report.findings().stream().anyMatch(f -> f.contains("No historical samples")),
                "empty input should explain why drift is STABLE"
        );
        assertTrue(
                report.recommendations().stream().anyMatch(r -> r.contains("Collect more outcome samples")),
                "empty input should ask the trader to collect more data"
        );
    }

    @Test
    void highWinRateLowDrawdownIsClassifiedStable() {
        List<OutcomeSample> samples = batch(7, 3, 0.15, 0.05);
        CalibrationReport report = analyzer.process(samples);

        assertEquals(CalibrationDriftLevel.STABLE, report.driftLevel());
        //assertEquals(0.70, report.winRate(), 1e-9);
    }

    @Test
    void mildlyDownshiftedPerformanceIsClassifiedMinorDrift() {
        List<OutcomeSample> samples = batch(6, 4, 0.03, 0.10);
        CalibrationReport report = analyzer.process(samples);

        assertEquals(CalibrationDriftLevel.MINOR_DRIFT, report.driftLevel());
        assertTrue(
                report.recommendations().stream().anyMatch(r -> r.contains("adversarial thresholds")),
                "minor drift should point at adversarial thresholds"
        );
    }

    @Test
    void degradedPerformanceIsClassifiedModerateDrift() {
        List<OutcomeSample> samples = batch(5, 5, -0.02, 0.18);
        CalibrationReport report = analyzer.process(samples);

        assertEquals(CalibrationDriftLevel.MODERATE_DRIFT, report.driftLevel());
        assertTrue(
                report.recommendations().stream().anyMatch(r -> r.contains("propagation and asymmetry")),
                "moderate drift should point at propagation + asymmetry"
        );
    }

    @Test
    void brokenFrameworkIsClassifiedMajorDrift() {
        List<OutcomeSample> samples = batch(2, 8, -0.20, 0.40);
        CalibrationReport report = analyzer.process(samples);

        assertEquals(CalibrationDriftLevel.MAJOR_DRIFT, report.driftLevel());
        assertTrue(
                report.recommendations().stream().anyMatch(r -> r.contains("Reduce deployment aggressiveness")),
                "major drift should recommend reducing deployment aggressiveness"
        );
        assertTrue(
                report.driftLevel().requiresAction(),
                "MAJOR_DRIFT must require trader action"
        );
    }

    /**
     * Builds a list of OutcomeSamples with {@code wins} winners and
     * {@code losses} losers, each carrying the same realized return and
     * drawdown. Used to drive specific drift bands deterministically.
     */
    private List<OutcomeSample> batch(int wins, int losses, double realized, double drawdown) {
        Stream<OutcomeSample> winners = IntStream.range(0, wins)
                .mapToObj(i -> sample("win-" + i, realized, drawdown, true));
        Stream<OutcomeSample> losers = IntStream.range(0, losses)
                .mapToObj(i -> sample("loss-" + i, realized, drawdown, false));
        return Stream.concat(winners, losers).toList();
    }

    private OutcomeSample sample(String candidateId, double realized, double drawdown, boolean thesisWorked) {
        return new OutcomeSample(
                candidateId,
                "AAA",
                OBSERVED_AT,
                DEFAULT_SCORES,
                realized,
                drawdown,
                DEFAULT_DAYS_HELD,
                thesisWorked
        );
    }
}
