package dev.reddragon.app.services.pipeline;

import dev.reddragon.analytics.services.meta.LongHorizonCalibrationAnalyzer;
import dev.reddragon.analytics.models.AnalyticsScoreBreakdown;
import dev.reddragon.analytics.models.OutcomeSample;
import dev.reddragon.persistence.domains.CalibrationOutcomeEntity;
import dev.reddragon.persistence.services.repositories.CalibrationOutcomeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CalibrationOutcomeServiceTest {

    @Test
    void appendsSamplesAndReturnsRollingReport() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAll()).thenReturn(List.of());

        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);
        service.analyzeAndAppend(List.of(sample("c1", "AAA", 0.12, true)));

        ArgumentCaptor<List<CalibrationOutcomeEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    private OutcomeSample sample(String id, String symbol, double realizedReturn, boolean worked) {
        return new OutcomeSample(id, symbol, Instant.now(),
                new AnalyticsScoreBreakdown(0.8, 0.7, 0.6, 0.7, 0.6, 0.7, 0.6, 0.8),
                realizedReturn, 0.1, 5, worked);
    }

    @Test
    void recentOutcomesRespectsLimit() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.2,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "BBB", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.3,5,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<OutcomeSample> outcomes = service.recentOutcomes(1);

        assertEquals(1, outcomes.size());
    }


    @Test
    void recentOutcomesForSymbolRespectsLimit() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.2,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.3,5,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<OutcomeSample> outcomes = service.recentOutcomesForSymbol("aaa", 1);

        assertEquals(1, outcomes.size());
    }


    @Test
    void exportRecentOutcomesCsvIncludesHeader() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.parse("2026-05-13T00:00:00Z"),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.2,4,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        String csv = service.exportRecentOutcomesCsv(10);

        assertTrue(csv.startsWith("candidateId,symbol,observedAt"));
        assertTrue(csv.contains("c1,AAA"));
    }


    @Test
    void summaryUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.10,0.2,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.05,0.1,5,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        CalibrationOutcomeService.CalibrationSummary summary = service.summary(100);

        assertEquals(2, summary.sampleSize());
        assertEquals(0.5, summary.winRate());
    }


    @Test
    void deleteOutcomesForSymbolNormalizes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.deleteBySymbol("AAA")).thenReturn(3L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        long deleted = service.deleteOutcomesForSymbol(" aaa ");

        assertEquals(3L, deleted);
    }


    @Test
    void summaryForSymbolUsesFilteredOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.10,0.2,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.30,0.1,5,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        CalibrationOutcomeService.CalibrationSummary summary = service.summaryForSymbol("aaa", 100);

        assertEquals(2, summary.sampleSize());
        assertEquals(1.0, summary.winRate());
    }


    @Test
    void clearAllOutcomesReturnsDeletedCount() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.count()).thenReturn(7L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        long deleted = service.clearAllOutcomes();

        verify(repository).deleteAllInBatch();
        assertEquals(7L, deleted);
    }


    @Test
    void countOutcomesFunctions() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.count()).thenReturn(11L);
        when(repository.countBySymbol("AAA")).thenReturn(4L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        assertEquals(11L, service.countOutcomes());
        assertEquals(4L, service.countOutcomesForSymbol("aaa"));
    }


    @Test
    void latestObservedAtReturnsIsoString() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTopByOrderByObservedAtDesc()).thenReturn(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.parse("2026-05-13T00:00:00Z"),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.2,4,true)
        );
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        String latest = service.latestObservedAt();

        assertEquals("2026-05-13T00:00:00Z", latest);
    }


    @Test
    void recentOutcomesPageUsesRepositoryPagination() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.2,4,true)
        )));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<OutcomeSample> page = service.recentOutcomesPage(0, 25);

        assertEquals(1, page.size());
    }


    @Test
    void symbolsReturnsDistinctList() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findDistinctSymbols()).thenReturn(List.of("AAA", "BBB"));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<String> symbols = service.symbols();

        assertEquals(List.of("AAA", "BBB"), symbols);
    }


    @Test
    void hasOutcomesForSymbolUsesCount() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.countBySymbol("AAA")).thenReturn(1L, 0L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        assertTrue(service.hasOutcomesForSymbol("aaa"));
        assertTrue(!service.hasOutcomesForSymbol("aaa"));
    }


    @Test
    void exportRecentOutcomesCsvForSymbolIncludesRows() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.parse("2026-05-13T00:00:00Z"),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.2,4,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        String csv = service.exportRecentOutcomesCsvForSymbol("aaa", 10);

        assertTrue(csv.contains("c1,AAA"));
    }


    @Test
    void topOutcomesByReturnUsesRepository() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByRealizedReturnDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.1,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "BBB", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.1,4,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<OutcomeSample> top = service.topOutcomesByReturn(1);

        assertEquals(1, top.size());
        assertEquals("c1", top.get(0).candidateId());
    }


    @Test
    void worstOutcomesByReturnUsesRepository() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByRealizedReturnAsc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c9", "ZZZ", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.4,0.2,4,false),
                new CalibrationOutcomeEntity(2L, "c8", "YYY", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.2,0.2,4,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<OutcomeSample> worst = service.worstOutcomesByReturn(1);

        assertEquals(1, worst.size());
        assertEquals("c9", worst.get(0).candidateId());
    }


    @Test
    void medianReturnComputesMiddleValue() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.1,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.1,0.1,4,true),
                new CalibrationOutcomeEntity(3L, "c3", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.1,4,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double median = service.medianReturn(100);

        assertEquals(0.2, median);
    }


    @Test
    void highDrawdownOutcomesFiltersByThreshold() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByMaxDrawdownDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.30,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "BBB", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.05,4,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        List<OutcomeSample> rows = service.highDrawdownOutcomes(0.15, 10);

        assertEquals(1, rows.size());
        assertEquals("c1", rows.get(0).candidateId());
    }


    @Test
    void averageDaysHeldUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.1,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.1,6,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double avg = service.averageDaysHeld(100);

        assertEquals(5.0, avg);
    }


    @Test
    void maxReturnUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.1,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.6,0.1,6,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double max = service.maxReturn(100);

        assertEquals(0.6, max);
    }


    @Test
    void minReturnUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.1,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.6,0.1,6,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double min = service.minReturn(100);

        assertEquals(-0.6, min);
    }


    @Test
    void maxDrawdownValueUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.12,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.6,0.44,6,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double maxDrawdown = service.maxDrawdownValue(100);

        assertEquals(0.44, maxDrawdown);
    }


    @Test
    void minDrawdownValueUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.12,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.6,0.44,6,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double minDrawdown = service.minDrawdownValue(100);

        assertEquals(0.12, minDrawdown);
    }


    @Test
    void winRateUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.12,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.6,0.44,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double winRate = service.winRate(100);

        assertEquals(0.5, winRate);
    }


    @Test
    void averageReturnForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.12,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.44,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double avg = service.averageReturnForSymbol("aaa", 100);

        assertEquals(0.3, avg);
    }


    @Test
    void winRateForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.12,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.44,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double winRate = service.winRateForSymbol("aaa", 100);

        assertEquals(0.5, winRate);
    }


    @Test
    void averageDrawdownForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double avg = service.averageDrawdownForSymbol("aaa", 100);

        assertEquals(0.2, avg);
    }


    @Test
    void maxDrawdownForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double max = service.maxDrawdownForSymbol("aaa", 100);

        assertEquals(0.3, max);
    }

    @Test
    void minDrawdownForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double min = service.minDrawdownForSymbol("aaa", 100);

        assertEquals(0.1, min);
    }

    @Test
    void minReturnForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.1,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double min = service.minReturnForSymbol("aaa", 100);

        assertEquals(-0.1, min);
    }

    @Test
    void maxReturnForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.1,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double max = service.maxReturnForSymbol("aaa", 100);

        assertEquals(0.4, max);
    }

    @Test
    void medianReturnForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.1,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false),
                new CalibrationOutcomeEntity(3L, "c3", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.20,5,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double median = service.medianReturnForSymbol("aaa", 100);

        assertEquals(0.2, median);
    }

    @Test
    void averageDaysHeldForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.1,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double averageDaysHeld = service.averageDaysHeldForSymbol("aaa", 100);

        assertEquals(5.0, averageDaysHeld);
    }

    @Test
    void hasOutcomesForSymbolUsesRepositoryCount() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.countBySymbol("AAA")).thenReturn(2L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        boolean hasOutcomes = service.hasOutcomesForSymbol("aaa");

        assertTrue(hasOutcomes);
        verify(repository).countBySymbol("AAA");
    }

    @Test
    void countOutcomesForSymbolReturnsZeroForBlankSymbol() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        long count = service.countOutcomesForSymbol("   ");

        assertEquals(0L, count);
        verify(repository, never()).countBySymbol(anyString());
    }

    @Test
    void medianDrawdownForSymbolUsesFilteredSet() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100BySymbolOrderByObservedAtDesc("AAA")).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,-0.1,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.4,0.30,6,false),
                new CalibrationOutcomeEntity(3L, "c3", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.20,5,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double medianDrawdown = service.medianDrawdownForSymbol("aaa", 100);

        assertEquals(0.2, medianDrawdown);
    }

    @Test
    void medianDrawdownUsesRecentOutcomes() {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.findTop100ByOrderByObservedAtDesc()).thenReturn(List.of(
                new CalibrationOutcomeEntity(1L, "c1", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.1,0.10,4,true),
                new CalibrationOutcomeEntity(2L, "c2", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.2,0.30,6,false),
                new CalibrationOutcomeEntity(3L, "c3", "AAA", Instant.now(),0.8,0.7,0.6,0.7,0.6,0.7,0.6,0.8,0.3,0.20,5,true)
        ));
        CalibrationOutcomeService service = new CalibrationOutcomeService(new LongHorizonCalibrationAnalyzer(), repository);

        double medianDrawdown = service.medianDrawdown(100);

        assertEquals(0.2, medianDrawdown);
    }

}
