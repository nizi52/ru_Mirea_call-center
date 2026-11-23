package ru.mirea.callcenter.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.mirea.callcenter.error.BadRequestException;

import java.util.Map;

/** Создаёт Pageable только из белого списка полей сортировки. */
@Component
public class PageableFactory {
    private static final Map<String, String> APPEAL_FIELDS = Map.of(
            "name", "clientName", "clientName", "clientName", "topic", "topic",
            "center", "center.code", "status", "status", "impact", "impactScore",
            "priority", "priorityLevel", "updated", "updatedAt");
    private static final Map<String, String> USER_FIELDS = Map.of(
            "email", "email", "name", "displayName", "created", "createdAt", "updated", "updatedAt");
    private static final Map<String, String> AUDIT_FIELDS = Map.of(
            "occurred", "occurredAt", "actor", "actorEmail", "event", "eventType");

    public Pageable appeals(int page, int size, String sort) { return create(page, size, sort, APPEAL_FIELDS, "clientName"); }
    public Pageable users(int page, int size, String sort) { return create(page, size, sort, USER_FIELDS, "email"); }
    public Pageable audit(int page, int size, String sort) { return create(page, size, sort, AUDIT_FIELDS, "occurredAt"); }

    private Pageable create(int page, int size, String sort, Map<String, String> allowed, String defaultField) {
        if (page < 0) throw new BadRequestException("page не может быть отрицательным");
        if (size < 1 || size > 100) throw new BadRequestException("size должен быть от 1 до 100");
        String[] parts = sort == null || sort.isBlank() ? new String[]{defaultField, "asc"} : sort.split(",", 2);
        String field = allowed.getOrDefault(parts[0], allowed.containsValue(parts[0]) ? parts[0] : null);
        if (field == null) throw new BadRequestException("Недопустимое поле сортировки");
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, field).and(Sort.by("id")));
    }
}
