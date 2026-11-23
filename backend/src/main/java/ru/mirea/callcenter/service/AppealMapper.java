package ru.mirea.callcenter.service;

import org.springframework.stereotype.Component;
import ru.mirea.callcenter.domain.Appeal;
import ru.mirea.callcenter.dto.AppealDtos.AppealResponse;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Преобразует entity в публичный DTO внутри активной транзакции. */
@Component
public class AppealMapper {
    public AppealResponse toResponse(Appeal appeal) {
        String fullName = appeal.getExternalId() + " · " + appeal.getTopic();
        return new AppealResponse(appeal.getId(), appeal.getExternalId(), appeal.getTopic(),
                appeal.getClientName(), appeal.getCategory(), fullName, appeal.getEmail(),
                appeal.getPhone(), appeal.getDueDate(),
                appeal.getCallCenter() == null ? null : appeal.getCallCenter().getId(),
                appeal.getCallCenter() == null ? null : appeal.getCallCenter().getCode(),
                appeal.getStatus(), appeal.getImpactScore(), appeal.isUrgent(), appeal.getPriorityLevel(),
                appeal.isArchived(), appeal.getArchivedAt(), Set.copyOf(appeal.getTags()),
                appeal.getCreatedAt(), appeal.getUpdatedAt(), appeal.getVersion(), appeal.getOperatorId());
    }
}
