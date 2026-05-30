package dev.reddragon.app.controllers;

import dev.reddragon.app.models.TickerAnalysisResponse;
import dev.reddragon.app.services.analysis.TickerAnalysisService;
import dev.reddragon.validation.config.ValidationProfile;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TickerAnalysisControllerTest {

    @Test
    void analyzeTickerPassesLookbackAndValidationProfileToService() throws Exception {
        TickerAnalysisService service = mock(TickerAnalysisService.class);
        when(service.analyze("asts", 30, ValidationProfile.CONSERVATIVE))
                .thenReturn(new TickerAnalysisResponse(
                        "ASTS",
                        "0001780312",
                        "SEC_MARKET",
                        null,
                        null,
                        null,
                        null,
                        java.util.List.of(),
                        java.util.List.of()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TickerAnalysisController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/analysis/asts?lookbackDays=30&profile=CONSERVATIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("ASTS"))
                .andExpect(jsonPath("$.analysisMode").value("SEC_MARKET"));

        verify(service).analyze("asts", 30, ValidationProfile.CONSERVATIVE);
    }
}
