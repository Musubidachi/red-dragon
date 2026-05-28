package dev.reddragon.app.controllers;

import dev.reddragon.app.services.pipeline.CalibrationOutcomeService;
import dev.reddragon.persistence.services.repositories.CalibrationOutcomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalibrationControllerTest {

    @Test
    void countEndpointReturnsDedicatedScalarShape() throws Exception {
        CalibrationOutcomeRepository repository = mock(CalibrationOutcomeRepository.class);
        when(repository.count()).thenReturn(12L);
        CalibrationOutcomeService service = new CalibrationOutcomeService(null, repository);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CalibrationController(service)).build();

        mockMvc.perform(get("/api/calibration/outcomes/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("outcomeCount"))
                .andExpect(jsonPath("$.count").value(12))
                .andExpect(jsonPath("$.sampleSize").doesNotExist())
                .andExpect(jsonPath("$.winRate").doesNotExist());
    }
}
