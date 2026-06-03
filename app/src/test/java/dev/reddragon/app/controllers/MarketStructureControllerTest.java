package dev.reddragon.app.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.reddragon.app.config.JacksonConfiguration;
import dev.reddragon.marketdata.services.IntradayStructureSnapshotBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MarketStructureControllerTest {

    @Test
    void processAcceptsIntradayBarJson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new MarketStructureController(new IntradayStructureSnapshotBuilder()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new JacksonConfiguration().objectMapper()))
                .build();

        mockMvc.perform(post("/api/market-structure/intraday")
                        .contentType("application/json")
                        .content("""
                                [
                                  {
                                    "symbol": "asts",
                                    "startTime": "2026-05-28T13:30:00Z",
                                    "open": 10.0,
                                    "high": 10.4,
                                    "low": 9.9,
                                    "close": 10.2,
                                    "volume": 1000,
                                    "vwap": 10.1
                                  },
                                  {
                                    "symbol": "asts",
                                    "startTime": "2026-05-28T13:35:00Z",
                                    "open": 10.2,
                                    "high": 10.8,
                                    "low": 10.1,
                                    "close": 10.7,
                                    "volume": 1400,
                                    "vwap": 10.4
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("ASTS"))
                .andExpect(jsonPath("$.aboveVwap").value(true))
                .andExpect(jsonPath("$.latestClose").value(10.7));
    }
}
