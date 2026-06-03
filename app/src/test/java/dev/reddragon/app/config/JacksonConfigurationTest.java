package dev.reddragon.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reddragon.app.models.BacktestRunResponse;
import dev.reddragon.backtest.models.BacktestMetrics;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.domain.models.AnalyticsSnapshot;
import dev.reddragon.domain.models.CalibrationDriftLevel;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.IntradayBar;
import dev.reddragon.domain.models.PhaseLabel;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.MarketDataSnapshot;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.domain.models.ReasonCode;
import dev.reddragon.domain.models.ValidationFactor;
import dev.reddragon.domain.models.ValidationResult;
import dev.reddragon.domain.models.ValidationStage;
import dev.reddragon.domain.models.Verdict;
import dev.reddragon.domain.models.exit.ExitSignalInput;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JacksonConfigurationTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    @Test
    void serializesFluentDomainValueObjects() throws Exception {
        MarketQuote quote = new MarketQuote(
                "ENPH",
                Instant.parse("2026-05-30T20:00:00Z"),
                42.50,
                42.45,
                42.55,
                1_500_000L,
                MarketDataQuality.COMPLETE,
                List.of("live")
        );

        CalibrationReport calibrationReport = new CalibrationReport(
                CalibrationDriftLevel.STABLE,
                0.71,
                0.08,
                0.11,
                List.of("Calibration remains steady."),
                List.of("Continue monitoring.")
        );

        TradeCandidate candidate = new TradeCandidate(
                "cand-1",
                "ENPH",
                "Enphase Energy",
                CandidateCatalystType.MANUAL_THESIS,
                SourceType.MANUAL,
                null,
                null,
                Instant.parse("2026-05-30T20:00:00Z"),
                "Headline",
                "Summary",
                0.72,
                0.68,
                0.66,
                0.70
        );
        MarketDataSnapshot marketData = new MarketDataSnapshot(
                "ENPH",
                Instant.parse("2026-05-30T20:00:00Z"),
                42.50,
                41.10,
                0.034,
                2.10,
                0.48,
                1_200_000.0,
                0.78,
                0.66,
                1.12,
                0.015,
                0.63,
                MarketDataQuality.COMPLETE,
                List.of()
        );
        AnalyticsSnapshot analytics = new AnalyticsSnapshot(
                "cand-1",
                "ENPH",
                Instant.parse("2026-05-30T20:00:00Z"),
                RegimeLabel.SUPPORTIVE_TREND,
                0.69,
                0.61,
                0.64,
                0.67,
                0.70,
                List.of("ok")
        );
        ValidationFactor factor = new ValidationFactor(
                ValidationStage.STRUCTURAL_REALITY,
                0.82,
                1.0,
                ReasonCode.STRUCTURAL_CATALYST_CONFIRMED,
                "Confirmed"
        );
        ValidationResult validation = new ValidationResult(
                "cand-1",
                "ENPH",
                Verdict.PASS,
                DeploymentTier.STANDARD,
                0.83,
                List.of(factor),
                List.of(ReasonCode.STRUCTURAL_CATALYST_CONFIRMED),
                List.of("Confirmed")
        );
        BacktestOutcome outcome = new BacktestOutcome(candidate, marketData, analytics, validation);
        BacktestReport report = new BacktestReport(
                "demo",
                BacktestMetrics.from(List.of(outcome)),
                List.of(outcome)
        );
        BacktestRunResponse response = new BacktestRunResponse("run-1", report);

        String quoteJson = objectMapper.writeValueAsString(quote);
        String calibrationJson = objectMapper.writeValueAsString(calibrationReport);
        String responseJson = objectMapper.writeValueAsString(response);

        assertThat(quoteJson).contains("\"symbol\":\"ENPH\"");
        assertThat(quoteJson).contains("\"lastPrice\":42.5");
        assertThat(calibrationJson).contains("\"driftLevel\":\"STABLE\"");
        assertThat(calibrationJson).contains("\"historicalWinRate\":0.71");
        assertThat(responseJson).contains("\"runId\":\"run-1\"");
        assertThat(responseJson).contains("\"strategyName\":\"demo\"");
        assertThat(responseJson).contains("\"candidateId\":\"cand-1\"");
        assertThat(responseJson).contains("\"latestClose\":42.5");
        assertThat(responseJson).contains("\"verdict\":\"PASS\"");
        assertThat(responseJson).contains("\"regimeLabel\":\"SUPPORTIVE_TREND\"");
        assertThat(responseJson).contains("\"score\":0.83");
        assertThat(responseJson).contains("\"averageScore\":0.83");
    }

    @Test
    void deserializesBarRequestBodies() throws Exception {
        IntradayBar intradayBar = objectMapper.readValue("""
                {
                  "symbol": "enph",
                  "startTime": "2026-05-28T13:30:00Z",
                  "open": 10.0,
                  "high": 10.5,
                  "low": 9.9,
                  "close": 10.2,
                  "volume": 1000,
                  "vwap": 10.1
                }
                """, IntradayBar.class);

        MarketBar marketBar = objectMapper.readValue("""
                {
                  "symbol": "enph",
                  "date": "2026-05-28",
                  "open": 10.0,
                  "high": 10.5,
                  "low": 9.9,
                  "close": 10.2,
                  "volume": 1000
                }
                """, MarketBar.class);

        assertThat(intradayBar.symbol()).isEqualTo("ENPH");
        assertThat(intradayBar.startTime()).isEqualTo(Instant.parse("2026-05-28T13:30:00Z"));
        assertThat(marketBar.symbol()).isEqualTo("ENPH");
        assertThat(marketBar.date()).isEqualTo(java.time.LocalDate.parse("2026-05-28"));
    }

    @Test
    void deserializesExitSignalInput() throws Exception {
        ExitSignalInput input = objectMapper.readValue("""
                {
                  "equilibriumPhase": "SATURATION_RISK",
                  "propagationPhase": "LATE_REFLEXIVITY",
                  "currentAsymmetry": 0.35,
                  "entryAsymmetry": 0.82,
                  "rangePosition": 0.92,
                  "nearRecentHigh": true
                }
                """, ExitSignalInput.class);

        assertThat(input.equilibriumPhase()).isEqualTo(PhaseLabel.SATURATION_RISK);
        assertThat(input.propagationPhase()).isEqualTo(PhaseLabel.LATE_REFLEXIVITY);
        assertThat(input.currentAsymmetry()).isEqualTo(0.35);
        assertThat(input.entryAsymmetry()).isEqualTo(0.82);
        assertThat(input.rangePosition()).isEqualTo(0.92);
        assertThat(input.nearRecentHigh()).isTrue();
    }
}
