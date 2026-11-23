package ru.mirea.callcenter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.callcenter.config.AppProperties;
import ru.mirea.callcenter.domain.Appeal;
import ru.mirea.callcenter.domain.AppealStatus;
import ru.mirea.callcenter.domain.CallCenter;
import ru.mirea.callcenter.domain.UserAccount;
import ru.mirea.callcenter.dto.AppealDtos.BulkAction;
import ru.mirea.callcenter.dto.AppealDtos.BulkFailure;
import ru.mirea.callcenter.dto.AppealDtos.BulkRequest;
import ru.mirea.callcenter.dto.AppealDtos.BulkResult;
import ru.mirea.callcenter.dto.AppealDtos.AppealRequest;
import ru.mirea.callcenter.dto.AppealDtos.AppealResponse;
import ru.mirea.callcenter.error.BadRequestException;
import ru.mirea.callcenter.error.ConflictException;
import ru.mirea.callcenter.error.NotFoundException;
import ru.mirea.callcenter.filter.SmartFilterParser;
import ru.mirea.callcenter.filter.AppealFilter;
import ru.mirea.callcenter.filter.AppealSpecifications;
import ru.mirea.callcenter.repository.AppealNoteRepository;
import ru.mirea.callcenter.repository.AppealRepository;
import ru.mirea.callcenter.repository.CallCenterRepository;
import ru.mirea.callcenter.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Основной прикладной сервис карточек обращений. */
@Service
public class AppealService {
    private final AppealRepository appeals;
    private final AppealNoteRepository notes;
    private final CallCenterRepository centers;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final AppealMapper mapper;
    private final PriorityCalculator priorityCalculator;
    private final AuditService audit;
    private final AppProperties properties;
    private final ru.mirea.callcenter.repository.CallClientRepository clients;
    private final org.springframework.transaction.support.TransactionTemplate rowTransaction;
    private final ru.mirea.callcenter.repository.CallOperatorRepository operators;

    public AppealService(AppealRepository appeals, AppealNoteRepository notes,
                          CallCenterRepository centers, CurrentUserService currentUser,
                          SmartFilterParser parser, AppealMapper mapper,
                          PriorityCalculator priorityCalculator, AuditService audit,
                          AppProperties properties, ru.mirea.callcenter.repository.CallOperatorRepository operators, org.springframework.transaction.PlatformTransactionManager tx, ru.mirea.callcenter.repository.CallClientRepository clients) {
        this.clients = clients;
        this.operators = operators;
        this.rowTransaction = new org.springframework.transaction.support.TransactionTemplate(tx);
        this.rowTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.appeals = appeals;
        this.notes = notes;
        this.centers = centers;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.priorityCalculator = priorityCalculator;
        this.audit = audit;
        this.properties = properties;
    }

    /** Выполняет безопасный умный поиск с пагинацией. */
    @Transactional(readOnly = true)
    public Page<AppealResponse> search(String query, Pageable pageable) {
        AppealFilter filter = parser.parse(query);
        return appeals.findAll(AppealSpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    /** Возвращает одну карточку независимо от архивного статуса. */
    @Transactional(readOnly = true)
    public AppealResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** Создаёт карточку и рассчитывает приоритет на сервере. */
    @Transactional
    public AppealResponse create(AppealRequest request) {
        UserAccount actor = currentUser.requireEntity();
        validateExternalId(request.externalId(), null);
        Appeal appeal = new Appeal(UUID.randomUUID(), normalizeRequired(request.topic()),
                normalizeRequired(request.clientName()), actor);
        apply(appeal, request, actor);
        appeal = appeals.saveAndFlush(appeal);
        audit.record("APPEAL_CREATED", "APPEAL", appeal.getId(),
                "Создана карточка обращения " + displayName(appeal), Map.of());
        appeals.flush();
        return mapper.toResponse(appeal);
    }

    /** Изменяет карточку только при совпадении optimistic version. */
    @Transactional
    public AppealResponse update(UUID id, AppealRequest request) {
        Appeal appeal = require(id);
        verifyVersion(request.version(), appeal.getVersion());
        validateExternalId(request.externalId(), id);
        UserAccount actor = currentUser.requireEntity();
        apply(appeal, request, actor);
        audit.record("APPEAL_UPDATED", "APPEAL", id,
                "Изменена карточка обращения " + displayName(appeal), Map.of("version", appeal.getVersion()));
        appeals.flush();
        return mapper.toResponse(appeal);
    }

    /** Архивирует карточку вместо физического удаления. */
    @Transactional
    public AppealResponse archive(UUID id) {
        Appeal appeal = require(id);
        archiveInternal(appeal, currentUser.requireEntity());
        audit.record("APPEAL_ARCHIVED", "APPEAL", id,
                "Карточка обращения перемещена в архив", Map.of());
        appeals.flush();
        return mapper.toResponse(appeal);
    }

    /** Возвращает карточку из архива. */
    @Transactional
    public AppealResponse restore(UUID id) {
        Appeal appeal = require(id);
        validateBusiness(appeal, appeal.getStatus(), appeal.getOperatorId(), appeal.getCallCenter()==null?null:appeal.getCallCenter().getId(), true);
        appeal.setArchived(false);
        appeal.setArchivedAt(null);
        appeal.setArchivedBy(null);
        appeal.setUpdatedBy(currentUser.requireEntity());
        audit.record("APPEAL_RESTORED", "APPEAL", id,
                "Карточка обращения восстановлена из архива", Map.of());
        appeals.flush();
        return mapper.toResponse(appeal);
    }

    /**
     * Безвозвратно удаляет запись только при включённом feature flag и явном подтверждении.
     */
    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Безвозвратное удаление отключено конфигурацией сервера");
        }
        if (!"PERMANENTLY_DELETE".equals(confirmation)) {
            throw new BadRequestException("Для удаления требуется заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        }
        Appeal appeal = require(id);
        String name = displayName(appeal);
        appeals.delete(appeal);
        audit.record("APPEAL_PERMANENTLY_DELETED", "APPEAL", id,
                "Безвозвратно удалена карточка " + name, Map.of());
    }

    /** Выполняет ограниченную массовую операцию и сообщает ошибки отдельных UUID. */
    public BulkResult bulk(BulkRequest request) {
        validateBulkRequest(request);
        int succeeded=0;
        var failures=new ArrayList<BulkFailure>();
        for(UUID id:request.ids()) {
            try {
                rowTransaction.executeWithoutResult(tx -> {
                    Appeal appeal=require(id);
                    applyBulk(appeal,request,currentUser.requireEntity());
                    appeals.flush();
                    audit.record("APPEAL_BULK_"+request.action(),"APPEAL",id,"Массовая операция",Map.of());
                });
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id,ex instanceof BadRequestException || ex instanceof NotFoundException || ex instanceof ConflictException ? ex.getMessage() : "Операция не выполнена: конфликт данных"));
            }
        }
        return new BulkResult(request.ids().size(),succeeded,failures);
    }

    /** Используется CSV-сервисом внутри транзакции без дублирования правил. */
    public boolean importRow(AppealRequest request, boolean createOnly) {
        return Boolean.TRUE.equals(rowTransaction.execute(tx -> {
            Appeal existing=appeals.findByExternalIdIgnoreCase(request.externalId()).orElse(null);
            if(existing!=null && createOnly) throw new ConflictException("Запись уже существует");
            UserAccount actor=currentUser.requireEntity();
            boolean created=existing==null;
            Appeal appeal=created?new Appeal(UUID.randomUUID(),normalizeRequired(request.topic()),normalizeRequired(request.clientName()),actor):existing;
            apply(appeal,request,actor);
            appeal = appeals.saveAndFlush(appeal);
            audit.record("APPEAL_IMPORTED","APPEAL",appeal.getId(),"Импорт CSV",Map.of());
            return created;
        }));
    }

    @Transactional(readOnly=true)
    public void validateImport(AppealRequest request) {
        PhoneNormalizer.normalize(request.phone());
        Appeal existing=appeals.findByExternalIdIgnoreCase(request.externalId()).orElse(null);
        Appeal a=existing==null?new Appeal(UUID.randomUUID(),request.topic(),request.clientName(),null):existing;
        if(existing==null && request.status()!=AppealStatus.NEW) throw new BadRequestException("Новая запись должна иметь статус NEW");
        if(!allowed(a.getStatus(),request.status())) throw new BadRequestException("Недопустимый переход статуса");
        resolveCallCenter(request.centerId(), a);
        if(request.status()!=AppealStatus.NEW && request.operatorId()==null) throw new BadRequestException("Нужен оператор");
        if(request.operatorId()!=null){
          var o=operators.findById(request.operatorId()).orElseThrow(()->new BadRequestException("Оператор не найден"));
          if(!request.operatorId().equals(a.getOperatorId()) && !o.isActive()) throw new BadRequestException("Оператор неактивен");
          if(!a.isArchived() && (request.status()==AppealStatus.NEW || request.status()==AppealStatus.IN_PROGRESS || request.status()==AppealStatus.ESCALATED) && appeals.activeLoad(request.operatorId(),a.getId())>=5) throw new BadRequestException("У оператора уже пять активных обращений");
        }
    }

    private void apply(Appeal appeal, AppealRequest request, UserAccount actor) {
        validateBusiness(appeal, request.status(), request.operatorId(), request.centerId());
        // Native phone lookup may flush pending changes: resolve before changing status/closedAt.
        String phone = PhoneNormalizer.normalize(request.phone());
        var client=clients.findByPhone(phone).orElseGet(()->clients.saveAndFlush(new ru.mirea.callcenter.domain.CallClient(normalizeRequired(request.clientName()),phone,normalizeEmail(request.email()))));
        appeal.setOperatorId(request.operatorId());
        if (request.status() == AppealStatus.CLOSED && appeal.getClosedAt() == null) appeal.setClosedAt(Instant.now());
        appeal.setExternalId(normalizeNullable(request.externalId()));
        appeal.setTopic(normalizeRequired(request.topic()));
        appeal.setClientName(normalizeRequired(request.clientName()));
        appeal.setCategory(normalizeNullable(request.category()));
        appeal.setEmail(normalizeEmail(request.email()));
        appeal.setPhone(PhoneNormalizer.normalize(request.phone()));
        appeal.setClientId(client.getId());
        appeal.setDueDate(request.dueDate());
        appeal.setCallCenter(resolveCallCenter(request.centerId(), appeal));
        appeal.setStatus(request.status());
        appeal.setImpactScore(request.impactScore());
        appeal.setUrgent(request.urgent());
        appeal.setTags(normalizeTags(request.tags()));
        appeal.setPriorityLevel(priorityCalculator.calculate(request.status(), request.impactScore(), request.urgent()));
        appeal.setUpdatedBy(actor);
    }

    private void applyBulk(Appeal appeal, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(appeal, actor);
            case RESTORE -> {
                validateBusiness(appeal, appeal.getStatus(), appeal.getOperatorId(), appeal.getCallCenter()==null?null:appeal.getCallCenter().getId(), true);
        appeal.setArchived(false);
                appeal.setArchivedAt(null);
                appeal.setArchivedBy(null);
                appeal.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                validateBusiness(appeal, request.status(), appeal.getOperatorId(), appeal.getCallCenter()==null?null:appeal.getCallCenter().getId());
                if(request.status()==AppealStatus.CLOSED && appeal.getClosedAt()==null) appeal.setClosedAt(Instant.now());
                appeal.setStatus(request.status());
                appeal.setPriorityLevel(priorityCalculator.calculate(appeal.getStatus(), appeal.getImpactScore(), appeal.isUrgent()));
                appeal.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(appeal.getTags());
                tags.add(normalizeTag(request.tag()));
                if(tags.size()>20) throw new BadRequestException("Допустимо не более 20 тегов");
                appeal.setTags(tags);
                appeal.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(appeal.getTags());
                tags.remove(normalizeTag(request.tag()));
                appeal.setTags(tags);
                appeal.setUpdatedBy(actor);
            }
        }
    }

    private static void validateBulkRequest(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для SET_STATUS требуется status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом требуется tag");
        }
    }

    private static void archiveInternal(Appeal appeal, UserAccount actor) {
        appeal.setArchived(true);
        appeal.setArchivedAt(Instant.now());
        appeal.setArchivedBy(actor);
        appeal.setUpdatedBy(actor);
    }

    public static boolean allowed(AppealStatus from, AppealStatus to) {
        return from==to || switch(from) {
          case NEW -> to==AppealStatus.IN_PROGRESS;
          case IN_PROGRESS -> to==AppealStatus.ESCALATED || to==AppealStatus.RESOLVED;
          case ESCALATED -> to==AppealStatus.RESOLVED;
          case RESOLVED -> to==AppealStatus.CLOSED;
          case CLOSED -> false;
        };
    }
    private void validateBusiness(Appeal a, AppealStatus status, Integer operatorId, UUID centerId) {
        validateBusiness(a, status, operatorId, centerId, false);
    }
    private void validateBusiness(Appeal a, AppealStatus status, Integer operatorId, UUID centerId, boolean restoring) {
        if(a.getCreatedAt()==null && status!=AppealStatus.NEW) throw new BadRequestException("Новое обращение должно иметь статус NEW");
        if(!allowed(a.getStatus(),status)) throw new BadRequestException("Недопустимый переход статуса");
        if(centerId==null) throw new BadRequestException("Выберите колл-центр");
        if(status!=AppealStatus.NEW && operatorId==null) throw new BadRequestException("Для работы и закрытия нужен оператор");
        if(operatorId!=null) {
            var operator=operators.lockById(operatorId).orElseThrow(()->new BadRequestException("Оператор не найден"));
            if(!operatorId.equals(a.getOperatorId()) && !operator.isActive()) throw new BadRequestException("Оператор неактивен");
            if((!a.isArchived() || restoring) && (status==AppealStatus.NEW || status==AppealStatus.IN_PROGRESS || status==AppealStatus.ESCALATED)
               && appeals.activeLoad(operatorId,a.getId())>=5) throw new BadRequestException("У оператора уже пять активных обращений");
        }
    }

    private Appeal require(UUID id) {
        return appeals.findById(id).orElseThrow(() -> new NotFoundException("Обращение не найдено"));
    }

    private CallCenter resolveCallCenter(UUID id, Appeal appeal) {
        if (id == null) throw new BadRequestException("Выберите колл-центр");
        CallCenter center = centers.findById(id).orElseThrow(() -> new BadRequestException("Колл-центр не найден"));
        if (!center.isActive() && (appeal.getCallCenter() == null || !id.equals(appeal.getCallCenter().getId()))) throw new BadRequestException("Нельзя назначить неактивный колл-центр");
        return center;
    }

    private void validateExternalId(String externalId, UUID currentId) {
        String normalized = normalizeNullable(externalId);
        if (normalized == null) return;
        boolean exists = currentId == null
                ? appeals.existsByExternalIdIgnoreCase(normalized)
                : appeals.existsByExternalIdIgnoreCaseAndIdNot(normalized, currentId);
        if (exists) throw new ConflictException("Внешний идентификатор уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Карточка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String displayName(Appeal appeal) {
        return appeal.getClientName() + " " + appeal.getTopic();
    }
}
