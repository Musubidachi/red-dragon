package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.Optional;

import dev.reddragon.persistence.domains.ValidationVerdictEntity;
import dev.reddragon.persistence.domains.VerdictOverrideEntity;
import dev.reddragon.persistence.services.repositories.ValidationVerdictRepository;
import dev.reddragon.persistence.services.repositories.VerdictOverrideRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VerdictOverrideControllerTest {

    @Test
    void overridePersistsValidatedVerdictOverride() throws Exception {
        ValidationVerdictRepository verdictRepository = mock(ValidationVerdictRepository.class);
        VerdictOverrideRepository overrideRepository = mock(VerdictOverrideRepository.class);
        ValidationVerdictEntity verdict = ValidationVerdictEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .verdict("WATCH")
                .deploymentTier("STANDARD")
                .score(0.64)
                .createdAt(Instant.parse("2026-05-28T12:00:00Z"))
                .build();
        ReflectionTestUtils.setField(verdict, "id", 42L);
        when(verdictRepository.findById(42L)).thenReturn(Optional.of(verdict));
        when(overrideRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new VerdictOverrideController(verdictRepository, overrideRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/verdicts/42/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"PASS\",\"reason\":\"Manual review\",\"author\":\"fm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictId").value(42))
                .andExpect(jsonPath("$.candidateId").value("cand-1"))
                .andExpect(jsonPath("$.originalVerdict").value("WATCH"))
                .andExpect(jsonPath("$.overrideVerdict").value("PASS"))
                .andExpect(jsonPath("$.reason").value("Manual review"));

        ArgumentCaptor<VerdictOverrideEntity> override = ArgumentCaptor.forClass(VerdictOverrideEntity.class);
        verify(overrideRepository).save(override.capture());
        assertThat(override.getValue().getVerdictId()).isEqualTo(42L);
        assertThat(override.getValue().getOverrideVerdict()).isEqualTo("PASS");
    }

    @Test
    void overrideRejectsUnknownVerdict() throws Exception {
        ValidationVerdictRepository verdictRepository = mock(ValidationVerdictRepository.class);
        VerdictOverrideRepository overrideRepository = mock(VerdictOverrideRepository.class);
        when(verdictRepository.findById(42L)).thenReturn(Optional.of(ValidationVerdictEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .verdict("WATCH")
                .deploymentTier("STANDARD")
                .score(0.64)
                .createdAt(Instant.parse("2026-05-28T12:00:00Z"))
                .build()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new VerdictOverrideController(verdictRepository, overrideRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/verdicts/42/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown verdict: MAYBE"));
    }
}
