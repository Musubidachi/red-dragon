package dev.reddragon.app.services.pipeline;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import dev.reddragon.domain.models.AnalyticsScoreBreakdown;
import dev.reddragon.domain.models.CalibrationReport;
import dev.reddragon.domain.models.OutcomeSample;
import dev.reddragon.analytics.services.meta.LongHorizonCalibrationAnalyzer;
import dev.reddragon.backtest.models.BacktestOutcome;
import dev.reddragon.persistence.domains.CalibrationOutcomeEntity;
import dev.reddragon.persistence.services.repositories.CalibrationOutcomeRepository;

@Service
public class CalibrationOutcomeService {

    private final LongHorizonCalibrationAnalyzer analyzer;
    private final CalibrationOutcomeRepository repository;

    public CalibrationOutcomeService(LongHorizonCalibrationAnalyzer analyzer, CalibrationOutcomeRepository repository) {
        this.analyzer = analyzer;
        this.repository = repository;
    }

    public synchronized CalibrationReport appendBacktestOutcomes(List<BacktestOutcome> outcomes) {
        repository.saveAll(outcomes.stream().map(this::toEntity).toList());
        return currentReport();
    }

    public synchronized CalibrationReport analyzeAndAppend(List<OutcomeSample> newSamples) {
        repository.saveAll(newSamples.stream().map(this::toEntity).toList());
        return currentReport();
    }

    public synchronized CalibrationReport currentReport() {
        List<OutcomeSample> samples = repository.findAll().stream().map(this::toSample).toList();
        return analyzer.process(samples);
    }


    public synchronized List<OutcomeSample> recentOutcomesForSymbol(String symbol, int limit) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        int bounded = Math.max(1, Math.min(limit, 100));
        return repository.findTop100BySymbolOrderByObservedAtDesc(symbol.trim().toUpperCase()).stream()
                .limit(bounded)
                .map(this::toSample)
                .toList();
    }












    public synchronized List<OutcomeSample> highDrawdownOutcomes(double minDrawdown, int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        return repository.findTop100ByOrderByMaxDrawdownDesc().stream()
                .filter(s -> s.getMaxDrawdown() >= minDrawdown)
                .limit(bounded)
                .map(this::toSample)
                .toList();
    }

    public synchronized List<OutcomeSample> worstOutcomesByReturn(int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        return repository.findTop100ByOrderByRealizedReturnAsc().stream()
                .limit(bounded)
                .map(this::toSample)
                .toList();
    }

    public synchronized List<OutcomeSample> topOutcomesByReturn(int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        return repository.findTop100ByOrderByRealizedReturnDesc().stream()
                .limit(bounded)
                .map(this::toSample)
                .toList();
    }

    public synchronized List<String> symbols() {
        return repository.findDistinctSymbols();
    }

    public synchronized List<OutcomeSample> recentOutcomesPage(int offset, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        int boundedOffset = Math.max(0, offset);
        int page = boundedOffset / boundedLimit;
        var pageable = PageRequest.of(page, boundedLimit, Sort.by(Sort.Direction.DESC, "observedAt"));
        return repository.findAll(pageable).stream().map(this::toSample).toList();
    }

    public synchronized String latestObservedAt() {
        var latest = repository.findTopByOrderByObservedAtDesc();
        return latest == null || latest.getObservedAt() == null ? "" : latest.getObservedAt().toString();
    }


    public synchronized boolean hasOutcomesForSymbol(String symbol) {
        return countOutcomesForSymbol(symbol) > 0;
    }

    public synchronized long countOutcomes() {
        return repository.count();
    }

    public synchronized long countOutcomesForSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return 0L;
        }
        return repository.countBySymbol(symbol.trim().toUpperCase());
    }

    public synchronized long clearAllOutcomes() {
        long count = repository.count();
        repository.deleteAllInBatch();
        return count;
    }

    public synchronized long deleteOutcomesForSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return 0L;
        }
        return repository.deleteBySymbol(symbol.trim().toUpperCase());
    }


    public synchronized CalibrationSummary summaryForSymbol(String symbol, int limit) {
        List<OutcomeSample> samples = recentOutcomesForSymbol(symbol, limit);
        if (samples.isEmpty()) {
            return new CalibrationSummary(0, 0.0, 0.0, 0.0);
        }
        long wins = samples.stream().filter(OutcomeSample::thesisWorked).count();
        double avgReturn = samples.stream().mapToDouble(OutcomeSample::realizedReturn).average().orElse(0.0);
        double avgDrawdown = samples.stream().mapToDouble(OutcomeSample::maxDrawdown).average().orElse(0.0);
        return new CalibrationSummary(samples.size(), wins / (double) samples.size(), avgReturn, avgDrawdown);
    }












    public synchronized double maxDrawdownForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToDouble(OutcomeSample::maxDrawdown)
                .max()
                .orElse(0.0);
    }

    public synchronized double minDrawdownForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToDouble(OutcomeSample::maxDrawdown)
                .min()
                .orElse(0.0);
    }

    public synchronized double averageDrawdownForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToDouble(OutcomeSample::maxDrawdown)
                .average()
                .orElse(0.0);
    }

    public synchronized double winRateForSymbol(String symbol, int limit) {
        List<OutcomeSample> samples = recentOutcomesForSymbol(symbol, limit);
        if (samples.isEmpty()) {
            return 0.0;
        }
        long wins = samples.stream().filter(OutcomeSample::thesisWorked).count();
        return wins / (double) samples.size();
    }

    public synchronized double averageReturnForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToDouble(OutcomeSample::realizedReturn)
                .average()
                .orElse(0.0);
    }

    public synchronized double minReturnForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToDouble(OutcomeSample::realizedReturn)
                .min()
                .orElse(0.0);
    }

    public synchronized double maxReturnForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToDouble(OutcomeSample::realizedReturn)
                .max()
                .orElse(0.0);
    }

    public synchronized double medianReturnForSymbol(String symbol, int limit) {
        List<Double> returns = recentOutcomesForSymbol(symbol, limit).stream()
                .map(OutcomeSample::realizedReturn)
                .sorted()
                .toList();
        if (returns.isEmpty()) {
            return 0.0;
        }
        int mid = returns.size() / 2;
        if (returns.size() % 2 == 0) {
            return (returns.get(mid - 1) + returns.get(mid)) / 2.0;
        }
        return returns.get(mid);
    }

    public synchronized double averageDaysHeldForSymbol(String symbol, int limit) {
        return recentOutcomesForSymbol(symbol, limit).stream()
                .mapToInt(OutcomeSample::daysHeld)
                .average()
                .orElse(0.0);
    }

    public synchronized double medianDrawdownForSymbol(String symbol, int limit) {
        List<Double> drawdowns = recentOutcomesForSymbol(symbol, limit).stream()
                .map(OutcomeSample::maxDrawdown)
                .sorted()
                .toList();
        if (drawdowns.isEmpty()) {
            return 0.0;
        }
        int mid = drawdowns.size() / 2;
        if (drawdowns.size() % 2 == 0) {
            return (drawdowns.get(mid - 1) + drawdowns.get(mid)) / 2.0;
        }
        return drawdowns.get(mid);
    }

    public synchronized double winRate(int limit) {
        List<OutcomeSample> samples = recentOutcomes(limit);
        if (samples.isEmpty()) {
            return 0.0;
        }
        long wins = samples.stream().filter(OutcomeSample::thesisWorked).count();
        return wins / (double) samples.size();
    }

    public synchronized double minDrawdownValue(int limit) {
        return recentOutcomes(limit).stream()
                .mapToDouble(OutcomeSample::maxDrawdown)
                .min()
                .orElse(0.0);
    }

    public synchronized double maxDrawdownValue(int limit) {
        return recentOutcomes(limit).stream()
                .mapToDouble(OutcomeSample::maxDrawdown)
                .max()
                .orElse(0.0);
    }

    public synchronized double minReturn(int limit) {
        return recentOutcomes(limit).stream()
                .mapToDouble(OutcomeSample::realizedReturn)
                .min()
                .orElse(0.0);
    }

    public synchronized double maxReturn(int limit) {
        return recentOutcomes(limit).stream()
                .mapToDouble(OutcomeSample::realizedReturn)
                .max()
                .orElse(0.0);
    }

    public synchronized double averageDaysHeld(int limit) {
        return recentOutcomes(limit).stream()
                .mapToInt(OutcomeSample::daysHeld)
                .average()
                .orElse(0.0);
    }

    public synchronized double medianReturn(int limit) {
        List<Double> returns = recentOutcomes(limit).stream()
                .map(OutcomeSample::realizedReturn)
                .sorted()
                .toList();
        if (returns.isEmpty()) {
            return 0.0;
        }
        int mid = returns.size() / 2;
        if (returns.size() % 2 == 0) {
            return (returns.get(mid - 1) + returns.get(mid)) / 2.0;
        }
        return returns.get(mid);
    }

    public synchronized double medianDrawdown(int limit) {
        List<Double> drawdowns = recentOutcomes(limit).stream()
                .map(OutcomeSample::maxDrawdown)
                .sorted()
                .toList();
        if (drawdowns.isEmpty()) {
            return 0.0;
        }
        int mid = drawdowns.size() / 2;
        if (drawdowns.size() % 2 == 0) {
            return (drawdowns.get(mid - 1) + drawdowns.get(mid)) / 2.0;
        }
        return drawdowns.get(mid);
    }

    public synchronized CalibrationSummary summary(int limit) {
        List<OutcomeSample> samples = recentOutcomes(limit);
        if (samples.isEmpty()) {
            return new CalibrationSummary(0, 0.0, 0.0, 0.0);
        }
        long wins = samples.stream().filter(OutcomeSample::thesisWorked).count();
        double avgReturn = samples.stream().mapToDouble(OutcomeSample::realizedReturn).average().orElse(0.0);
        double avgDrawdown = samples.stream().mapToDouble(OutcomeSample::maxDrawdown).average().orElse(0.0);
        return new CalibrationSummary(samples.size(), wins / (double) samples.size(), avgReturn, avgDrawdown);
    }

    public record CalibrationSummary(int sampleSize, double winRate, double averageReturn, double averageDrawdown) {}


    public synchronized String exportRecentOutcomesCsvForSymbol(String symbol, int limit) {
        List<OutcomeSample> outcomes = recentOutcomesForSymbol(symbol, limit);
        StringBuilder csv = new StringBuilder();
        csv.append("candidateId,symbol,observedAt,realizedReturn,maxDrawdown,daysHeld,thesisWorked\n");
        for (OutcomeSample o : outcomes) {
            csv.append(o.candidateId()).append(',')
                    .append(o.symbol()).append(',')
                    .append(o.observedAt()).append(',')
                    .append(o.realizedReturn()).append(',')
                    .append(o.maxDrawdown()).append(',')
                    .append(o.daysHeld()).append(',')
                    .append(o.thesisWorked()).append('\n');
        }
        return csv.toString();
    }

    public synchronized String exportRecentOutcomesCsv(int limit) {
        List<OutcomeSample> outcomes = recentOutcomes(limit);
        StringBuilder csv = new StringBuilder();
        csv.append("candidateId,symbol,observedAt,realizedReturn,maxDrawdown,daysHeld,thesisWorked\n");
        for (OutcomeSample s : outcomes) {
            csv.append(s.candidateId()).append(',')
                    .append(s.symbol()).append(',')
                    .append(s.observedAt()).append(',')
                    .append(s.realizedReturn()).append(',')
                    .append(s.maxDrawdown()).append(',')
                    .append(s.daysHeld()).append(',')
                    .append(s.thesisWorked()).append('\n');
        }
        return csv.toString();
    }

    public synchronized List<OutcomeSample> recentOutcomes(int limit) {
        int bounded = Math.max(1, Math.min(limit, 100));
        return repository.findTop100ByOrderByObservedAtDesc().stream()
                .limit(bounded)
                .map(this::toSample)
                .toList();
    }


    private CalibrationOutcomeEntity toEntity(BacktestOutcome outcome) {
        boolean thesisWorked = outcome.validation() != null && outcome.validation().passed();
        double proxyReturn = (outcome.validation() == null ? 0.0 : outcome.validation().score()) - 0.5;
        double eq = outcome.analytics() == null ? 0.5 : outcome.analytics().equilibriumQualityScore();
        double refl = outcome.analytics() == null ? 0.5 : outcome.analytics().reflexivityPotentialScore();
        double asym = outcome.analytics() == null ? 0.5 : outcome.analytics().asymmetryScore();
        double regime = outcome.analytics() == null ? 0.5 : outcome.analytics().regimeCompatibilityScore();
        double deploy = outcome.analytics() == null ? 0.5 : outcome.analytics().deploymentConfidenceScore();
        double proxyDrawdown = Math.max(0.0, 1.0 - eq);
        return new CalibrationOutcomeEntity(null, outcome.candidate().candidateId(), outcome.candidate().symbol(), Instant.now(),
                outcome.candidate().structuralRealityScore(), outcome.candidate().materialSignificanceScore(), outcome.candidate().earlynessScore(),
                eq, refl, asym, regime, deploy, proxyReturn, proxyDrawdown, 1, thesisWorked);
    }

    private CalibrationOutcomeEntity toEntity(OutcomeSample s) {
        AnalyticsScoreBreakdown b = s.scoreBreakdown();
        return new CalibrationOutcomeEntity(null, s.candidateId(), s.symbol(), s.observedAt(),
                b.structuralRealityScore(), b.materialSignificanceScore(), b.earlynessScore(), b.equilibriumQualityScore(),
                b.reflexivityPotentialScore(), b.asymmetryScore(), b.regimeCompatibilityScore(), b.deploymentConfidenceScore(),
                s.realizedReturn(), s.maxDrawdown(), s.daysHeld(), s.thesisWorked());
    }

    private OutcomeSample toSample(CalibrationOutcomeEntity e) {
        return new OutcomeSample(e.getCandidateId(), e.getSymbol(), e.getObservedAt(),
                new AnalyticsScoreBreakdown(e.getStructuralRealityScore(), e.getMaterialSignificanceScore(), e.getEarlynessScore(),
                        e.getEquilibriumQualityScore(), e.getReflexivityPotentialScore(), e.getAsymmetryScore(),
                        e.getRegimeCompatibilityScore(), e.getDeploymentConfidenceScore()),
                e.getRealizedReturn(), e.getMaxDrawdown(), e.getDaysHeld(), e.isThesisWorked());
    }
}
