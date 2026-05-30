package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dev.reddragon.analytics.services.MarketStateClassifier;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import dev.reddragon.persistence.domains.MarketStateSnapshotEntity;
import dev.reddragon.persistence.services.repositories.MarketStateSnapshotRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketStateControllerTest {

    @Test
    void snapshotsCanBeFilteredBySymbol() throws Exception {
        MarketStateSnapshotRepository snapshotRepository = mock(MarketStateSnapshotRepository.class);
        MarketStateSnapshotEntity snapshot = MarketStateSnapshotEntity.builder()
                .symbol("ASTS")
                .observedAt(Instant.parse("2026-05-28T14:00:00Z"))
                .source("SCHEDULED_INTRADAY")
                .regimeLabel("SUPPORTIVE_TREND")
                .confidence(0.75)
                .equilibriumRestorationProbability(0.61)
                .deploymentSupported(true)
                .sessionVwap(10.5)
                .latestClose(10.9)
                .vwapDistancePercent(0.038)
                .vwapReclaimStrength(0.8)
                .directionalPersistenceScore(0.7)
                .rotationalQualityScore(0.3)
                .intradayTrendStrength(0.65)
                .aboveVwap(true)
                .barCount(12)
                .notes("Deployment supported.")
                .build();
        when(snapshotRepository.findBySymbolOrderByObservedAtDesc(eq("ASTS"), any(Pageable.class)))
                .thenReturn(List.of(snapshot));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarketStateController(
                new IntradayStructureSnapshotBuilder(),
                new MarketStateClassifier(),
                snapshotRepository)).build();

        mockMvc.perform(get("/api/market-state/snapshots?symbol=asts&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("ASTS"))
                .andExpect(jsonPath("$[0].source").value("SCHEDULED_INTRADAY"))
                .andExpect(jsonPath("$[0].barCount").value(12));
    }
}
