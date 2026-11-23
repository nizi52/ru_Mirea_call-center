package ru.mirea.callcenter.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.callcenter.domain.Role;
import ru.mirea.callcenter.domain.Appeal;
import ru.mirea.callcenter.domain.AppealNote;
import ru.mirea.callcenter.domain.UserAccount;
import ru.mirea.callcenter.dto.NoteDtos.NoteRequest;
import ru.mirea.callcenter.dto.NoteDtos.NoteResponse;
import ru.mirea.callcenter.error.ConflictException;
import ru.mirea.callcenter.error.NotFoundException;
import ru.mirea.callcenter.repository.AppealNoteRepository;
import ru.mirea.callcenter.repository.AppealRepository;
import ru.mirea.callcenter.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Управляет заметками и авторскими правами на их изменение. */
@Service
public class NoteService {
    private final AppealNoteRepository notes;
    private final AppealRepository appeals;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(AppealNoteRepository notes, AppealRepository appeals,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.appeals = appeals;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID appealId) {
        requireAppeal(appealId);
        return notes.findByAppealIdOrderByPinnedDescCreatedAtDesc(appealId).stream().map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID appealId, NoteRequest request) {
        Appeal appeal = requireAppeal(appealId);
        UserAccount actor = currentUser.requireEntity();
        AppealNote note = new AppealNote(UUID.randomUUID(), appeal, actor, request.body().trim(), request.pinned());
        note = notes.saveAndFlush(note);
        audit.record("NOTE_CREATED", "APPEAL_NOTE", note.getId(), "Добавлена заметка к обращению", Map.of("appealId", appealId));
        notes.flush();
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID appealId, UUID noteId, NoteRequest request) {
        AppealNote note = requireNote(appealId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || request.version() != note.getVersion())
            throw new ConflictException("Заметка уже изменена. Обновите данные");
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("NOTE_UPDATED", "APPEAL_NOTE", noteId, "Изменена заметка", Map.of("appealId", appealId));
        notes.flush();
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID appealId, UUID noteId) {
        AppealNote note = requireNote(appealId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        notes.delete(note);
        audit.record("NOTE_DELETED", "APPEAL_NOTE", noteId, "Удалена заметка", Map.of("appealId", appealId));
    }

    private Appeal requireAppeal(UUID id) {
        return appeals.findById(id).orElseThrow(() -> new NotFoundException("Обращение не найдено"));
    }

    private AppealNote requireNote(UUID appealId, UUID noteId) {
        AppealNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (!note.getAppeal().getId().equals(appealId)) throw new NotFoundException("Заметка не найдена");
        return note;
    }

    private static void assertOwnerOrAdmin(AppealNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Only note author or administrator can modify it");
        }
    }

    private static NoteResponse toResponse(AppealNote note) {
        return new NoteResponse(note.getId(), note.getAppeal().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
