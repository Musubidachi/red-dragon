package dev.reddragon.app.config;

import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.DeploymentTier;
import dev.reddragon.domain.models.MarketDataQuality;
import dev.reddragon.domain.models.ReasonCode;
import dev.reddragon.domain.models.RegimeLabel;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.Verdict;
import dev.reddragon.persistence.domains.AnalyticsSnapshotEntity;
import dev.reddragon.persistence.domains.BacktestResultEntity;
import dev.reddragon.persistence.domains.CalibrationOutcomeEntity;
import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.MarketBarEntity;
import dev.reddragon.persistence.domains.MarketSnapshotEntity;
import dev.reddragon.persistence.domains.TraderNoteEntity;
import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.ValidationVerdictReasonEntity;
import dev.reddragon.persistence.services.repositories.AnalyticsSnapshotRepository;
import dev.reddragon.persistence.services.repositories.BacktestResultRepository;
import dev.reddragon.persistence.services.repositories.CalibrationOutcomeRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.MarketBarRepository;
import dev.reddragon.persistence.services.repositories.MarketSnapshotRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Seeds realistic demo data for the default local H2 in-memory database.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "red-dragon.sample-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class H2SampleDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(H2SampleDataLoader.class);

    private final DataSource dataSource;
    private final CandidateRepository candidateRepository;
    private final ValidationVerdictRepository verdictRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final MarketSnapshotRepository marketSnapshotRepository;
    private final MarketBarRepository marketBarRepository;
    private final TraderNoteRepository traderNoteRepository;
    private final CalibrationOutcomeRepository calibrationOutcomeRepository;
    private final BacktestResultRepository backtestResultRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!isH2InMemoryDatabase() || candidateRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now();

        List<CandidateEntity> candidates = List.of(
                candidate("demo-nvda-ai-infra", "NVDA", "NVIDIA Corp.",
                        CandidateCatalystType.STRUCTURAL_DEMAND_CHANGE, SourceType.MANUAL,
                        now.minusSeconds(45 * 60L),
                        "AI infrastructure demand remains supply constrained",
                        "Data-center demand and supply bottlenecks keep the disequilibrium intact."),
                candidate("demo-asts-contract", "ASTS", "AST SpaceMobile Inc.",
                        CandidateCatalystType.CONTRACT, SourceType.SEC_EDGAR,
                        now.minusSeconds(2 * 60 * 60L),
                        "Commercial network agreement expands addressable service footprint",
                        "Objective filing-backed contract catalyst with early propagation risk."),
                candidate("demo-enph-policy", "ENPH", "Enphase Energy Inc.",
                        CandidateCatalystType.POLICY_CHANGE, SourceType.NEWS_RSS,
                        now.minusSeconds(5 * 60 * 60L),
                        "Policy support improves residential solar financing backdrop",
                        "Policy catalyst is credible but market structure remains mixed."),
                candidate("demo-snow-earnings", "SNOW", "Snowflake Inc.",
                        CandidateCatalystType.NEWS_EVENT, SourceType.SCANNER,
                        now.minusSeconds(30 * 60 * 60L),
                        "Earnings reaction looks late after a broad gap",
                        "Useful reject sample outside the default 24-hour review window.")
        );
        candidateRepository.saveAll(candidates);

        marketBarRepository.saveAll(seedBars("NVDA", 128.0));
        marketBarRepository.saveAll(seedBars("ASTS", 27.0));
        marketBarRepository.saveAll(seedBars("ENPH", 119.0));

        marketSnapshotRepository.saveAll(List.of(
                marketSnapshot("demo-nvda-ai-infra", "NVDA", now.minusSeconds(40 * 60L),
                        135.80, 129.10, 5.19, 4.10, 0.73, 58_000_000.0,
                        0.86, 0.74, 1.45, 0.018, 0.68,
                        "High liquidity with controlled volatility after the initial gap."),
                marketSnapshot("demo-asts-contract", "ASTS", now.minusSeconds(110 * 60L),
                        29.40, 27.80, 5.76, 1.22, 0.66, 14_500_000.0,
                        0.69, 0.61, 1.72, 0.026, 0.58,
                        "Volume confirms attention, but range position needs monitoring."),
                marketSnapshot("demo-enph-policy", "ENPH", now.minusSeconds(4 * 60 * 60L),
                        124.20, 121.90, 1.89, 3.80, 0.54, 5_800_000.0,
                        0.57, 0.49, 1.08, -0.004, 0.42,
                        "Liquidity is acceptable, volatility stability is still mixed.")
        ));

        analyticsSnapshotRepository.saveAll(List.of(
                analytics("demo-nvda-ai-infra", "NVDA", now.minusSeconds(38 * 60L),
                        RegimeLabel.SUPPORTIVE_TREND, 0.84, 0.79, 0.82, 0.86, 0.88,
                        "Trend support, favorable asymmetry, and reflexivity still forming."),
                analytics("demo-asts-contract", "ASTS", now.minusSeconds(105 * 60L),
                        RegimeLabel.SUPPORTIVE_ROTATIONAL, 0.73, 0.68, 0.70, 0.76, 0.69,
                        "Rotational support is present, but deployment should stay measured."),
                analytics("demo-enph-policy", "ENPH", now.minusSeconds(4 * 60 * 60L),
                        RegimeLabel.MIXED, 0.55, 0.58, 0.56, 0.51, 0.52,
                        "Policy support exists, but equilibrium quality is not clean yet.")
        ));

        verdictRepository.saveAll(List.of(
                verdict("demo-nvda-ai-infra", "NVDA", Verdict.PASS, DeploymentTier.CONCENTRATED,
                        0.86, now.minusSeconds(36 * 60L),
                        List.of(
                                ReasonCode.STRUCTURAL_CATALYST_CONFIRMED,
                                ReasonCode.ASYMMETRY_FAVORABLE,
                                ReasonCode.REGIME_SUPPORTIVE),
                        List.of(
                                "Objective demand catalyst is intact.",
                                "Move has not fully repriced the upside.",
                                "Broader regime supports trend continuation.")),
                verdict("demo-asts-contract", "ASTS", Verdict.WATCH, DeploymentTier.PROBE,
                        0.67, now.minusSeconds(95 * 60L),
                        List.of(
                                ReasonCode.MATERIAL_IMPACT_MEDIUM,
                                ReasonCode.EARLY_EMERGING_PROPAGATION,
                                ReasonCode.DEPLOYMENT_PROBE_ONLY),
                        List.of(
                                "Contract impact is meaningful but sizing is uncertain.",
                                "Narrative is visible but not mainstream.",
                                "Setup merits a probe rather than full review.")),
                verdict("demo-enph-policy", "ENPH", Verdict.WATCH, DeploymentTier.OBSERVE,
                        0.59, now.minusSeconds(3 * 60 * 60L),
                        List.of(
                                ReasonCode.REGIME_NEUTRAL,
                                ReasonCode.ASYMMETRY_COMPRESSED),
                        List.of(
                                "Policy backdrop helps, but regime is mixed.",
                                "Current price already reflects part of the catalyst.")),
                verdict("demo-snow-earnings", "SNOW", Verdict.REJECT, DeploymentTier.NONE,
                        0.42, now.minusSeconds(30 * 60 * 60L),
                        List.of(
                                ReasonCode.MAINSTREAM_SATURATION,
                                ReasonCode.ASYMMETRY_UNFAVORABLE),
                        List.of(
                                "The move is already broadly discovered.",
                                "Reward/risk compressed after the gap."))
        ));

        traderNoteRepository.saveAll(List.of(
                note("demo-nvda-ai-infra", "NVDA", "Watch for continuation volume above the prior high.", now.minusSeconds(25 * 60L)),
                note("demo-asts-contract", "ASTS", "Keep sizing small until follow-through confirms liquidity depth.", now.minusSeconds(80 * 60L)),
                note("demo-enph-policy", "ENPH", "Needs a cleaner market tape before upgrade from observe.", now.minusSeconds(2 * 60 * 60L))
        ));

        calibrationOutcomeRepository.saveAll(List.of(
                outcome("hist-nvda-1", "NVDA", now.minusSeconds(20 * 24 * 60 * 60L), 0.88, 0.07, 18, true),
                outcome("hist-asts-1", "ASTS", now.minusSeconds(18 * 24 * 60 * 60L), 0.21, 0.13, 12, true),
                outcome("hist-enph-1", "ENPH", now.minusSeconds(16 * 24 * 60 * 60L), -0.06, 0.16, 9, false),
                outcome("hist-pltr-1", "PLTR", now.minusSeconds(12 * 24 * 60 * 60L), 0.14, 0.08, 15, true),
                outcome("hist-snow-1", "SNOW", now.minusSeconds(8 * 24 * 60 * 60L), -0.04, 0.11, 6, false),
                outcome("hist-crwd-1", "CRWD", now.minusSeconds(4 * 24 * 60 * 60L), 0.09, 0.05, 10, true)
        ));

        backtestResultRepository.saveAll(List.of(
                backtest("demo-run-001", "DisequilibriumV1", "NVDA", "demo-nvda-ai-infra", Verdict.PASS, 0.86, now.minusSeconds(15 * 60L)),
                backtest("demo-run-001", "DisequilibriumV1", "ASTS", "demo-asts-contract", Verdict.WATCH, 0.67, now.minusSeconds(15 * 60L)),
                backtest("demo-run-001", "DisequilibriumV1", "ENPH", "demo-enph-policy", Verdict.WATCH, 0.59, now.minusSeconds(15 * 60L))
        ));

        log.info("Seeded local H2 sample data: {} candidates, {} calibration outcomes",
                candidates.size(), 6);
    }

    private boolean isH2InMemoryDatabase() {
        try (var connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            return url != null && url.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:mem:");
        } catch (SQLException e) {
            log.warn("Unable to inspect datasource URL; skipping sample data seed", e);
            return false;
        }
    }

    private CandidateEntity candidate(
            String candidateId,
            String symbol,
            String companyName,
            CandidateCatalystType catalystType,
            SourceType sourceType,
            Instant observedAt,
            String headline,
            String summary
    ) {
        return CandidateEntity.builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .companyName(companyName)
                .catalystType(catalystType.name())
                .sourceType(sourceType.name())
                .sourceId(candidateId)
                .sourceUrl("https://example.test/" + candidateId)
                .observedAt(observedAt)
                .headline(headline)
                .summary(summary)
                .build();
    }

    private MarketSnapshotEntity marketSnapshot(
            String candidateId,
            String symbol,
            Instant observedAt,
            double latestClose,
            double previousClose,
            double gapPercent,
            double averageTrueRange,
            double rangePosition,
            double averageVolume,
            double liquidityScore,
            double volatilityStabilityScore,
            double relativeVolume,
            double vwapDeviation,
            double directionalPersistence,
            String notes
    ) {
        return new MarketSnapshotEntity(
                null, candidateId, symbol, observedAt,
                latestClose, previousClose, gapPercent, averageTrueRange,
                rangePosition, averageVolume, liquidityScore, volatilityStabilityScore,
                MarketDataQuality.COMPLETE.name(), notes,
                relativeVolume, vwapDeviation, directionalPersistence);
    }

    private AnalyticsSnapshotEntity analytics(
            String candidateId,
            String symbol,
            Instant observedAt,
            RegimeLabel regimeLabel,
            double regimeCompatibilityScore,
            double asymmetryScore,
            double equilibriumQualityScore,
            double reflexivityPotentialScore,
            double deploymentConfidenceScore,
            String reasonNotes
    ) {
        return new AnalyticsSnapshotEntity(
                null, candidateId, symbol, observedAt, regimeLabel.name(),
                regimeCompatibilityScore, asymmetryScore, equilibriumQualityScore,
                reflexivityPotentialScore, deploymentConfidenceScore, reasonNotes);
    }

    private ValidationVerdictEntity verdict(
            String candidateId,
            String symbol,
            Verdict verdict,
            DeploymentTier deploymentTier,
            double score,
            Instant createdAt,
            List<ReasonCode> reasonCodes,
            List<String> explanations
    ) {
        ValidationVerdictEntity entity = ValidationVerdictEntity.builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .verdict(verdict.name())
                .deploymentTier(deploymentTier.name())
                .score(score)
                .idempotencyKey("seed:" + candidateId)
                .reasonCodes(null)
                .explanations(null)
                .createdAt(createdAt)
                .reasons(new ArrayList<>())
                .build();

        for (int i = 0; i < reasonCodes.size(); i++) {
            entity.getReasons().add(new ValidationVerdictReasonEntity(
                    null,
                    entity,
                    reasonCodes.get(i).name(),
                    i < explanations.size() ? explanations.get(i) : null,
                    (short) i
            ));
        }
        return entity;
    }

    private TraderNoteEntity note(String candidateId, String symbol, String text, Instant createdAt) {
        return TraderNoteEntity.builder()
                .candidateId(candidateId)
                .symbol(symbol)
                .noteText(text)
                .createdAt(createdAt)
                .author("local-demo")
                .build();
    }

    private CalibrationOutcomeEntity outcome(
            String candidateId,
            String symbol,
            Instant observedAt,
            double realizedReturn,
            double maxDrawdown,
            int daysHeld,
            boolean thesisWorked
    ) {
        return new CalibrationOutcomeEntity(
                null, candidateId, symbol, observedAt,
                0.82, 0.74, 0.69, 0.71,
                0.67, 0.73, 0.66, 0.76,
                realizedReturn, maxDrawdown, daysHeld, thesisWorked);
    }

    private BacktestResultEntity backtest(
            String runId,
            String strategyName,
            String symbol,
            String candidateId,
            Verdict verdict,
            double score,
            Instant testedAt
    ) {
        return BacktestResultEntity.builder()
                .runId(runId)
                .strategyName(strategyName)
                .symbol(symbol)
                .candidateId(candidateId)
                .verdict(verdict.name())
                .score(score)
                .testedAt(testedAt)
                .build();
    }

    private List<MarketBarEntity> seedBars(String symbol, double base) {
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        List<MarketBarEntity> bars = new ArrayList<>();
        for (int i = 19; i >= 0; i--) {
            double close = base + (19 - i) * 0.55;
            double open = close - 0.25;
            bars.add(new MarketBarEntity(
                    null,
                    symbol,
                    end.minusDays(i),
                    open,
                    close + 0.80,
                    open - 0.70,
                    close,
                    1_000_000L + (19 - i) * 45_000L,
                    null
            ));
        }
        return bars;
    }
}
