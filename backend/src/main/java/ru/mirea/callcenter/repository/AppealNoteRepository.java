package ru.mirea.callcenter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.callcenter.domain.AppealNote;

import java.util.List;
import java.util.UUID;

/** Persistence gateway for appeal notes. */
public interface AppealNoteRepository extends JpaRepository<AppealNote, UUID> {
    List<AppealNote> findByAppealIdOrderByPinnedDescCreatedAtDesc(UUID appealId);
}
