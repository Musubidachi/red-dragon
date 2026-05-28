package dev.reddragon.app.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import dev.reddragon.analytics.services.DeterministicAnalyticsService;
import dev.reddragon.analytics.services.marketscoring.MarketDataSnapshotScorer;
import dev.reddragon.app.models.BacktestBarRequest;
import dev.reddragon.app.models.BacktestFrameRequest;
import dev.reddragon.app.models.BacktestRequest;
import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import dev.reddragon.backtest.models.BacktestReport;
import dev.reddragon.backtest.services.BacktestReplayEngine;
import dev.reddragon.ingestion.services.ManualCandidateIngestionService;
import dev.reddragon.marketdata.services.MarketFeatureCalculator;
import dev.reddragon.persistence.services.PersistenceMapper;
import dev.reddragon.persistence.services.repositories.BacktestResultRepository;
import dev.reddragon.persistence.services.repositories.CalibrationOutcomeRepository;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.validation.services.engine.CandidateValidationInputFactory;
import dev.reddragon.validation.services.engine.DisequilibriumValidationEngine;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "red-dragon.sample-data.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:reddragon-backtest-wiring;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        }
)
class BacktestControllerWiringTest {

    @Autowired
    private BacktestReplayEngine backtestReplayEngine;

    @Autowired
    private BacktestController backtestController;

    @Autowired
    private MarketFeatureCalculator marketFeatureCalculator;

    @Autowired
    private MarketDataSnapshotScorer marketDataSnapshotScorer;

    @Autowired
    private DeterministicAnalyticsService analyticsService;

    @Autowired
    private DisequilibriumValidationEngine validationEngine;

    @Autowired
    private CandidateValidationInputFactory validationInputFactory;

    @Autowired
    private ManualCandidateIngestionService ingestionService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private BacktestResultRepository backtestResultRepository;

    @Autowired
    private CalibrationOutcomeService calibrationOutcomeService;

    @Autowired
    private PersistenceMapper persistenceMapper;

    @Autowired
    private CalibrationOutcomeRepository calibrationOutcomeRepository;

    @Test
    void backtestReplayEngineUsesAppPipelineBeans() {
        assertSame(
                marketFeatureCalculator,
                ReflectionTestUtils.getField(backtestReplayEngine, "marketFeatureCalculator"));
        assertSame(
                marketDataSnapshotScorer,
                ReflectionTestUtils.getField(backtestReplayEngine, "marketDataSnapshotScorer"));
        assertSame(
                analyticsService,
                ReflectionTestUtils.getField(backtestReplayEngine, "analyticsService"));
        assertSame(
                validationEngine,
                ReflectionTestUtils.getField(backtestReplayEngine, "validationEngine"));
        assertSame(
                validationInputFactory,
                ReflectionTestUtils.getField(backtestReplayEngine, "validationInputFactory"));
    }

    @Test
    void backtestControllerUsesReplayAndPersistenceBeans() {
        assertSame(
                backtestReplayEngine,
                ReflectionTestUtils.getField(backtestController, "backtestReplayEngine"));
        assertSame(
                ingestionService,
                ReflectionTestUtils.getField(backtestController, "ingestionService"));
        assertSame(
                candidateRepository,
                ReflectionTestUtils.getField(backtestController, "candidateRepository"));
        assertSame(
                backtestResultRepository,
                ReflectionTestUtils.getField(backtestController, "backtestResultRepository"));
        assertSame(
                calibrationOutcomeService,
                ReflectionTestUtils.getField(backtestController, "calibrationOutcomeService"));
        assertSame(
                persistenceMapper,
                ReflectionTestUtils.getField(backtestController, "persistenceMapper"));

        BacktestReport report = backtestController.runBacktest(backtestRequest());

        assertEquals("wiring-smoke", report.strategyName());
        assertEquals(1, report.metrics().totalFrames());
        assertEquals(1, report.outcomes().size());
        assertEquals(1, candidateRepository.count());
        assertEquals(1, backtestResultRepository.count());
        assertEquals(1, calibrationOutcomeRepository.count());
    }

    private BacktestRequest backtestRequest() {
        BacktestRequest request = new BacktestRequest();
        request.setStrategyName("wiring-smoke");
        request.setFrames(List.of(backtestFrame()));
        return request;
    }

    private BacktestFrameRequest backtestFrame() {
        BacktestFrameRequest frame = new BacktestFrameRequest();
        frame.setSymbol("NVDA");
        frame.setCompanyName("NVIDIA Corporation");
        frame.setCatalystType("MANUAL_THESIS");
        frame.setHeadline("Backtest wiring smoke candidate");
        frame.setSummary("Minimal app-side wiring fixture.");
        frame.setStructuralRealityScore(0.80);
        frame.setMaterialSignificanceScore(0.75);
        frame.setEarlynessScore(0.65);
        frame.setReflexivityPotentialScore(0.70);
        frame.setBars(List.of(
                bar(LocalDate.of(2026, 1, 2), 100.00, 103.00, 99.00, 102.00, 1_000_000L),
                bar(LocalDate.of(2026, 1, 3), 102.00, 106.00, 101.00, 105.00, 1_200_000L),
                bar(LocalDate.of(2026, 1, 4), 105.00, 108.00, 104.00, 107.00, 1_300_000L)
        ));
        return frame;
    }

    private BacktestBarRequest bar(
            LocalDate date,
            double open,
            double high,
            double low,
            double close,
            long volume
    ) {
        BacktestBarRequest bar = new BacktestBarRequest();
        bar.setDate(date);
        bar.setOpen(open);
        bar.setHigh(high);
        bar.setLow(low);
        bar.setClose(close);
        bar.setVolume(volume);
        return bar;
    }
}
