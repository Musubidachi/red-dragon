package dev.reddragon.app.controllers;

import java.time.Instant;
import java.util.Optional;

import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.TraderNoteEntity;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
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

class TraderNoteControllerTest {

    @Test
    void addNotePersistsNoteForCandidate() throws Exception {
        CandidateRepository candidateRepository = mock(CandidateRepository.class);
        TraderNoteRepository noteRepository = mock(TraderNoteRepository.class);
        CandidateEntity candidate = CandidateEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .catalystType("CONTRACT")
                .sourceType("MANUAL")
                .observedAt(Instant.parse("2026-05-28T12:00:00Z"))
                .build();
        when(candidateRepository.findById("cand-1")).thenReturn(Optional.of(candidate));
        when(noteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TraderNoteController(candidateRepository, noteRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/candidates/cand-1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Watch for follow-through.\",\"author\":\"fm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value("cand-1"))
                .andExpect(jsonPath("$.symbol").value("ASTS"))
                .andExpect(jsonPath("$.noteText").value("Watch for follow-through."))
                .andExpect(jsonPath("$.author").value("fm"));

        ArgumentCaptor<TraderNoteEntity> note = ArgumentCaptor.forClass(TraderNoteEntity.class);
        verify(noteRepository).save(note.capture());
        assertThat(note.getValue().getCandidateId()).isEqualTo("cand-1");
        assertThat(note.getValue().getNoteText()).isEqualTo("Watch for follow-through.");
    }

    @Test
    void addNoteRejectsBlankNote() throws Exception {
        CandidateRepository candidateRepository = mock(CandidateRepository.class);
        TraderNoteRepository noteRepository = mock(TraderNoteRepository.class);
        when(candidateRepository.findById("cand-1")).thenReturn(Optional.of(CandidateEntity.builder()
                .candidateId("cand-1")
                .symbol("ASTS")
                .catalystType("CONTRACT")
                .sourceType("MANUAL")
                .observedAt(Instant.parse("2026-05-28T12:00:00Z"))
                .build()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TraderNoteController(candidateRepository, noteRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/candidates/cand-1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("note must not be blank"));
    }
}
