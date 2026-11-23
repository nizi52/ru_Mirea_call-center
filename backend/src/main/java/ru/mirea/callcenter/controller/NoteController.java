package ru.mirea.callcenter.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.callcenter.dto.NoteDtos.NoteRequest;
import ru.mirea.callcenter.dto.NoteDtos.NoteResponse;
import ru.mirea.callcenter.service.NoteService;

import java.util.List;
import java.util.UUID;

/** REST-контур редакторских заметок. */
@RestController
@RequestMapping("/api/appeals/{appealId}/notes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) { this.notes = notes; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN')")
    public List<NoteResponse> list(@PathVariable UUID appealId) { return notes.list(appealId); }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@PathVariable UUID appealId,
                                               @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(appealId, request));
    }

    @PutMapping("/{noteId}")
    public NoteResponse update(@PathVariable UUID appealId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(appealId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable UUID appealId, @PathVariable UUID noteId) {
        notes.delete(appealId, noteId);
        return ResponseEntity.noContent().build();
    }
}
