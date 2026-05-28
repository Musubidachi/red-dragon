package dev.reddragon.app.controllers;

import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateControllerTest {

    @Test
    void listCandidatesReturnsListingDto() throws Exception {
        CandidateRepository repository = mock(CandidateRepository.class);
        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .companyName("AST SpaceMobile")
                .catalystType("CONTRACT")
                .sourceType("SEC_EDGAR")
                .sourceId("src-1")
                .sourceUrl("https://example.test/1")
                .observedAt(Instant.parse("2026-05-27T12:00:00Z"))
                .headline("Headline")
                .summary("Summary")
                .createdAt(Instant.parse("2026-05-27T12:01:00Z"))
                .updatedAt(Instant.parse("2026-05-27T12:02:00Z"))
                .version(7L)
                .build();
        when(repository.findAllByOrderByObservedAtDesc(any()))
                .thenReturn(new PageImpl<>(List.of(candidate), PageRequest.of(0, 25), 1));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CandidateController(repository)).build();

        mockMvc.perform(get("/api/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].candidateId").value("cand-1"))
                .andExpect(jsonPath("$.content[0].symbol").value("ASTS"))
                .andExpect(jsonPath("$.content[0].createdAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.content[0].version").doesNotExist());
    }
}
