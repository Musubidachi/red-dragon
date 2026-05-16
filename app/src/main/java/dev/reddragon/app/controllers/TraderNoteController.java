package dev.reddragon.app.controllers;

import dev.reddragon.persistence.domains.CandidateEntity;
import dev.reddragon.persistence.domains.TraderNoteEntity;
import dev.reddragon.persistence.services.repositories.CandidateRepository;
import dev.reddragon.persistence.services.repositories.TraderNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Freeform trader annotations attached to a candidate.
 *
 * <pre>
 * POST /api/candidates/{id}/notes   — add a note
 * GET  /api/candidates/{id}/notes   — list all notes
 * </pre>
 */
@RestController
@RequestMapping("/api/candidates/{id}/notes")
@RequiredArgsConstructor
public class TraderNoteController {

    private final CandidateRepository candidateRepository;
    private final TraderNoteRepository traderNoteRepository;

    @PostMapping
    public ResponseEntity<TraderNoteEntity> addNote(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        CandidateEntity candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + id));

        String noteText = body.getOrDefault("note", "").trim();
        if (noteText.isEmpty()) {
            throw new IllegalArgumentException("note must not be blank");
        }
        String author = body.get("author");

        TraderNoteEntity note = new TraderNoteEntity(
                null,
                candidate.getCandidateId(),
                candidate.getSymbol(),
                noteText,
                Instant.now(),
                author
        );
        return ResponseEntity.ok(traderNoteRepository.save(note));
    }

    @GetMapping
    public List<TraderNoteEntity> getNotes(@PathVariable String id) {
        return traderNoteRepository.findByCandidateIdOrderByCreatedAtDesc(id);
    }
}
