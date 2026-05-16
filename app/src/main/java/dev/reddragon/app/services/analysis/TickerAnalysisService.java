package dev.reddragon.app.services.analysis;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import dev.reddragon.app.models.PipelineRunResult;
import dev.reddragon.app.models.SourceCoverage;
import dev.reddragon.app.models.SourceCoverageStatus;
import dev.reddragon.app.models.TickerAnalysisResponse;
import dev.reddragon.app.models.TickerResearchSummary;
import dev.reddragon.app.services.pipeline.CandidatePipelineOrchestrator;
import dev.reddragon.domain.models.CandidateCatalystType;
import dev.reddragon.domain.models.SourceType;
import dev.reddragon.domain.models.TradeCandidate;
import dev.reddragon.ingestion.services.ManualCandidateIngestionService;
import dev.reddragon.ingestion.services.sec.CikLookupService;
import dev.reddragon.ingestion.services.sec.SecIngestionService;
import dev.reddragon.domain.models.MarketBar;
import dev.reddragon.domain.models.MarketQuote;
import dev.reddragon.marketdata.services.provider.MarketDataProvider;
import dev.reddragon.validation.config.ValidationProfile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TickerAnalysisService {

    private static final int MAX_LOOKBACK_DAYS = 365;

    private final CikLookupService cikLookupService;
    private final SecIngestionService secIngestionService;
    private final MarketDataProvider marketDataProvider;
    private final CandidatePipelineOrchestrator orchestrator;
    private final ManualCandidateIngestionService manualCandidateIngestionService;
    private final TickerResearchClient researchClient;

    public TickerAnalysisResponse analyze(String ticker, int lookbackDays, ValidationProfile profile) {
        String symbol = normalizeTicker(ticker);
        List<String> notes = new ArrayList<>();
        List<MarketBar> bars = dailyBars(symbol, lookbackDays, notes);
        MarketQuote quote = quote(symbol, notes);
        String cik = cik(symbol, notes);
        List<TradeCandidate> secCandidates = secCandidates(symbol, cik, notes);
        TickerResearchSummary research = researchClient.research(symbol, quote, bars, secCandidates);

        TradeCandidate selected = selectedCandidate(symbol, secCandidates, research);
        PipelineRunResult result = orchestrator.process(uniqueAnalysisCandidate(selected), bars, profile);
        SourceCoverage sourceCoverage = sourceCoverage(bars, quote, cik, secCandidates, research);

        String analysisMode = analysisMode(cik, secCandidates, research);
        return new TickerAnalysisResponse(
                symbol,
                cik,
                analysisMode,
                sourceCoverage,
                selected,
                result,
                research,
                secCandidates,
                List.copyOf(notes)
        );
    }

    private List<MarketBar> dailyBars(String symbol, int lookbackDays, List<String> notes) {
        int safeLookback = Math.max(5, Math.min(lookbackDays, MAX_LOOKBACK_DAYS));
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(safeLookback);
        try {
            List<MarketBar> bars = marketDataProvider.historicalDailyBars(symbol, from, to).stream()
                    .sorted(Comparator.comparing(MarketBar::date))
                    .toList();
            if (bars.isEmpty()) {
                notes.add("No daily market bars were available; market features will be conservative.");
            }
            return bars;
        } catch (RuntimeException error) {
            notes.add("Daily market data failed: " + error.getMessage());
            return List.of();
        }
    }

    private MarketQuote quote(String symbol, List<String> notes) {
        try {
            MarketQuote quote = marketDataProvider.quote(symbol);
            if (!quote.available()) {
                notes.add("Live quote unavailable: " + String.join("; ", quote.notes()));
            }
            return quote;
        } catch (RuntimeException error) {
            notes.add("Quote lookup failed: " + error.getMessage());
            return MarketQuote.unavailable(symbol, error.getMessage());
        }
    }

    private String cik(String symbol, List<String> notes) {
        try {
            return cikLookupService.process(symbol).orElseGet(() -> {
                notes.add("No SEC CIK was found for " + symbol + ".");
                return null;
            });
        } catch (RuntimeException error) {
            notes.add("SEC ticker lookup failed: " + error.getMessage());
            return null;
        }
    }

    private List<TradeCandidate> secCandidates(String symbol, String cik, List<String> notes) {
        if (cik == null || cik.isBlank()) {
            return List.of();
        }
        try {
            return secIngestionService.process(cik).stream()
                    .filter(candidate -> symbol.equals(candidate.symbol()))
                    .limit(5)
                    .toList();
        } catch (RuntimeException error) {
            notes.add("SEC candidate ingestion failed: " + error.getMessage());
            return List.of();
        }
    }

    private TradeCandidate selectedCandidate(
            String symbol,
            List<TradeCandidate> secCandidates,
            TickerResearchSummary research
    ) {
        if (!secCandidates.isEmpty()) {
            return secCandidates.get(0);
        }
        CandidateCatalystType catalystType = research.catalystType() == null
                ? CandidateCatalystType.MANUAL_THESIS
                : research.catalystType();
        String headline = textOrDefault(research.headline(), "Ticker analysis for " + symbol);
        String summary = textOrDefault(research.summary(), "No recent factual catalyst was found.");
        return manualCandidateIngestionService.process(
                symbol,
                symbol,
                catalystType,
                headline,
                summary,
                research.structuralRealityScore(),
                research.materialSignificanceScore(),
                research.earlynessScore(),
                research.reflexivityPotentialScore()
        );
    }

    private TradeCandidate uniqueAnalysisCandidate(TradeCandidate candidate) {
        return candidate.toBuilder()
                .candidateId("analysis-" + UUID.randomUUID())
                .sourceType(candidate.sourceType() == SourceType.SEC_EDGAR ? SourceType.SEC_EDGAR : SourceType.MANUAL)
                .build();
    }

    private String analysisMode(String cik, List<TradeCandidate> secCandidates, TickerResearchSummary research) {
        if (!secCandidates.isEmpty() && research.llmEnabled()) {
            return "SEC_MARKET_LLM";
        }
        if (!secCandidates.isEmpty()) {
            return "SEC_MARKET";
        }
        if (cik != null && research.llmEnabled()) {
            return "MARKET_LLM_SEC_LOOKUP";
        }
        if (research.llmEnabled()) {
            return "MARKET_LLM";
        }
        return "MARKET_ONLY";
    }

    private SourceCoverage sourceCoverage(
            List<MarketBar> bars,
            MarketQuote quote,
            String cik,
            List<TradeCandidate> secCandidates,
            TickerResearchSummary research
    ) {
        SourceCoverageStatus marketData = marketDataCoverage(bars, quote);
        SourceCoverageStatus secFilings = secCoverage(cik, secCandidates);
        SourceCoverageStatus llmResearch = llmCoverage(research);

        List<String> missingSources = new ArrayList<>();
        List<String> degradedSources = new ArrayList<>();
        addSourceStatus("marketData", marketData, missingSources, degradedSources);
        addSourceStatus("secFilings", secFilings, missingSources, degradedSources);
        addSourceStatus("llmResearch", llmResearch, missingSources, degradedSources);

        double confidence = weightedConfidence(marketData, secFilings, llmResearch);
        return new SourceCoverage(
                marketData,
                secFilings,
                llmResearch,
                confidence,
                missingSources,
                degradedSources
        );
    }

    private SourceCoverageStatus marketDataCoverage(List<MarketBar> bars, MarketQuote quote) {
        boolean hasEnoughBars = bars != null && bars.size() >= 14;
        boolean hasSomeBars = bars != null && !bars.isEmpty();
        boolean hasQuote = quote != null && quote.available();
        if (hasEnoughBars && hasQuote) {
            return SourceCoverageStatus.AVAILABLE;
        }
        if (hasSomeBars || hasQuote) {
            return SourceCoverageStatus.DEGRADED;
        }
        return SourceCoverageStatus.UNAVAILABLE;
    }

    private SourceCoverageStatus secCoverage(String cik, List<TradeCandidate> secCandidates) {
        if (secCandidates != null && !secCandidates.isEmpty()) {
            return SourceCoverageStatus.AVAILABLE;
        }
        if (cik != null && !cik.isBlank()) {
            return SourceCoverageStatus.DEGRADED;
        }
        return SourceCoverageStatus.UNAVAILABLE;
    }

    private SourceCoverageStatus llmCoverage(TickerResearchSummary research) {
        if (research == null || !research.llmEnabled()) {
            return SourceCoverageStatus.UNAVAILABLE;
        }
        if (research.sources() == null || research.sources().isEmpty()) {
            return SourceCoverageStatus.DEGRADED;
        }
        return SourceCoverageStatus.AVAILABLE;
    }

    private void addSourceStatus(
            String source,
            SourceCoverageStatus status,
            List<String> missingSources,
            List<String> degradedSources
    ) {
        if (status == SourceCoverageStatus.UNAVAILABLE) {
            missingSources.add(source);
        } else if (status == SourceCoverageStatus.DEGRADED) {
            degradedSources.add(source);
        }
    }

    private double weightedConfidence(
            SourceCoverageStatus marketData,
            SourceCoverageStatus secFilings,
            SourceCoverageStatus llmResearch
    ) {
        return (0.45 * sourceScore(marketData))
                + (0.25 * sourceScore(secFilings))
                + (0.30 * sourceScore(llmResearch));
    }

    private double sourceScore(SourceCoverageStatus status) {
        return switch (status) {
            case AVAILABLE -> 1.0;
            case DEGRADED -> 0.55;
            case UNAVAILABLE -> 0.15;
        };
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker is required");
        }
        return ticker.trim().toUpperCase(Locale.ROOT);
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
