package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.List;

import dev.reddragon.persistence.domains.TradeHistoryImportBatchEntity;
import dev.reddragon.persistence.domains.TradeHistoryRecordEntity;
import dev.reddragon.persistence.services.repositories.TradeHistoryImportBatchRepository;
import dev.reddragon.persistence.services.repositories.TradeHistoryRecordRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradeHistoryImportControllerTest {

    @Test
    void importCsvPersistsBatchAndNormalizedTrades() throws Exception {
        TradeHistoryImportBatchRepository batchRepository = mock(TradeHistoryImportBatchRepository.class);
        TradeHistoryRecordRepository recordRepository = mock(TradeHistoryRecordRepository.class);
        when(batchRepository.save(any())).thenReturn(TradeHistoryImportBatchEntity.builder()
                .importedAt(Instant.parse("2026-05-27T12:00:00Z"))
                .totalRows(1)
                .importedRows(1)
                .warnings("")
                .build());
        MockMvc mockMvc = mockMvc(new TradeHistoryImportController(batchRepository, recordRepository));
        String csv = "timestamp,ticker,side,quantity,price,realizedPnL,account\\n"
                + "2026-05-27T12:00:00Z,asts,buy,10,22.50,15.25,main";

        mockMvc.perform(post("/api/history/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"csv\":\"" + csv + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.importedRows").value(1))
                .andExpect(jsonPath("$.trades[0].ticker").value("ASTS"))
                .andExpect(jsonPath("$.warnings[0]").value(
                        "Strategy and market-state heuristic fields are deprecated and are no longer inferred during import."));

        ArgumentCaptor<List<TradeHistoryRecordEntity>> records = ArgumentCaptor.forClass(List.class);
        verify(recordRepository).saveAll(records.capture());
        assertThat(records.getValue()).hasSize(1);
        assertThat(records.getValue().getFirst().getTicker()).isEqualTo("ASTS");
        assertThat(records.getValue().getFirst().getStrategyType()).isNull();
        assertThat(records.getValue().getFirst().getMarketState()).isNull();
    }

    @Test
    void tradesFiltersTickerAndLookback() throws Exception {
        TradeHistoryImportBatchRepository batchRepository = mock(TradeHistoryImportBatchRepository.class);
        TradeHistoryRecordRepository recordRepository = mock(TradeHistoryRecordRepository.class);
        TradeHistoryRecordEntity record = TradeHistoryRecordEntity.builder()
                .importBatchId(1L)
                .tradeTimestamp(Instant.parse("2026-05-27T12:00:00Z"))
                .ticker("ASTS")
                .side("BUY")
                .quantity(10)
                .price(22.50)
                .account("main")
                .build();
        when(recordRepository.findByTickerAndTradeTimestampAfterOrderByTradeTimestampDesc(
                org.mockito.ArgumentMatchers.eq("ASTS"),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(record));
        MockMvc mockMvc = mockMvc(new TradeHistoryImportController(batchRepository, recordRepository));

        mockMvc.perform(get("/api/history/trades?ticker=asts&lookbackHours=24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("ASTS"))
                .andExpect(jsonPath("$[0].strategyType").doesNotExist())
                .andExpect(jsonPath("$[0].marketState").doesNotExist());
    }

    private MockMvc mockMvc(TradeHistoryImportController controller) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }
}
