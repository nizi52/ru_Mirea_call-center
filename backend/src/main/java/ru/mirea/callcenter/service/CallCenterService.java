package ru.mirea.callcenter.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.callcenter.domain.CallCenter;
import ru.mirea.callcenter.dto.CallCenterDtos.CallCenterRequest;
import ru.mirea.callcenter.dto.CallCenterDtos.CallCenterResponse;
import ru.mirea.callcenter.error.BadRequestException;
import ru.mirea.callcenter.error.ConflictException;
import ru.mirea.callcenter.error.NotFoundException;
import ru.mirea.callcenter.repository.CallCenterRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Прикладные операции над колл-центрами. */
@Service
public class CallCenterService {
    private final CallCenterRepository centers;
    private final AuditService audit;

    public CallCenterService(CallCenterRepository centers, AuditService audit) {
        this.centers = centers;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CallCenterResponse> list(boolean activeOnly) {
        return (activeOnly ? centers.findByActiveTrueOrderByCodeAsc() : centers.findAllByOrderByCodeAsc())
                .stream().map(CallCenterService::toResponse).toList();
    }

    @Transactional
    public CallCenterResponse create(CallCenterRequest request) {
        String code = normalizeCode(request.code());
        if (centers.existsByCodeIgnoreCase(code)) throw new ConflictException("Колл-центр с таким кодом уже существует");
        CallCenter center = new CallCenter(UUID.randomUUID(), code, normalizeNullable(request.name()), request.slaHours());
        if (request.active() != null) center.setActive(request.active());
        center = centers.saveAndFlush(center);
        audit.record("CENTER_CREATED", "CENTER", center.getId(), "Создан колл-центр " + code, Map.of());
        centers.flush();
        return toResponse(center);
    }

    @Transactional
    public CallCenterResponse update(UUID id, CallCenterRequest request) {
        CallCenter center = require(id);
        if (request.version() == null || request.version() != center.getVersion())
            throw new ConflictException("Колл-центр уже изменён. Обновите список");
        String code = normalizeCode(request.code());
        centers.findByCodeIgnoreCase(code).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Колл-центр с таким кодом уже существует");
        });
        center.setCode(code);
        center.setName(normalizeNullable(request.name()));
        center.setSlaHours(request.slaHours());
        if (request.active() != null) center.setActive(request.active());
        audit.record("CENTER_UPDATED", "CENTER", id, "Изменён колл-центр " + code, Map.of());
        centers.flush();
        return toResponse(center);
    }

    @Transactional
    public CallCenterResponse setActive(UUID id, boolean active, long version) {
        CallCenter center = require(id);
        if (version != center.getVersion()) throw new ConflictException("Колл-центр уже изменён. Обновите список");
        center.setActive(active);
        audit.record(active ? "CENTER_ACTIVATED" : "CENTER_DEACTIVATED", "CENTER", id,
                (active ? "Активирован" : "Деактивирован") + " колл-центр " + center.getCode(), Map.of());
        centers.flush();
        return toResponse(center);
    }

    private CallCenter require(UUID id) {
        return centers.findById(id).orElseThrow(() -> new NotFoundException("Колл-центр не найден"));
    }

    private static CallCenterResponse toResponse(CallCenter center) {
        return new CallCenterResponse(center.getId(), center.getCode(), center.getName(), center.getSlaHours(),
                center.isActive(), center.getCreatedAt(), center.getUpdatedAt(), center.getVersion());
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Код колл-центра обязателен");
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
