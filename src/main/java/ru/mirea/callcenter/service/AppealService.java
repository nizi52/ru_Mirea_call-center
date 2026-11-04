package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.*;
import ru.mirea.callcenter.model.*;
import ru.mirea.callcenter.repository.AppealRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class AppealService {
    private static final Set<String> CATEGORIES = Set.copyOf(categories());
    private static final int MAX_TOPIC_LENGTH = 300;
    private static final int MAX_ACTIVE_PER_OPERATOR = 5;

    private final AppealRepository repository;
    private final OperatorService operatorService;
    private final ClientService clientService;

    public AppealService(AppealRepository repository, OperatorService operatorService, ClientService clientService) {
        this.repository = repository;
        this.operatorService = operatorService;
        this.clientService = clientService;
    }

    public static List<String> categories() { return List.of("TECH", "COMPLAINT", "CONSULTATION", "BILLING"); }

    // ---------- CRUD ----------

    public List<Appeal> getAll() throws SQLException { return repository.findAll(); }

    public Appeal getById(int id) throws SQLException {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Обращение с ID " + id + " не найдено."));
    }

    public Appeal create(Appeal appeal) throws SQLException {
        appeal.setStatus(AppealStatus.NEW);
        appeal.setCreatedAt(LocalDateTime.now());
        appeal.setClosedAt(null);
        validateFields(appeal);
        validateAssignment(appeal, null);
        return repository.save(appeal);
    }

    public Appeal updateDetails(Appeal edited) throws SQLException {
        Appeal old = getById(edited.getId());
        edited.setStatus(old.getStatus());
        edited.setCreatedAt(old.getCreatedAt());
        edited.setClosedAt(old.getClosedAt());
        validateFields(edited);
        validateAssignment(edited, old);
        return repository.save(edited);
    }

    public Appeal changeStatus(int id, AppealStatus next) throws SQLException {
        Appeal appeal = getById(id);
        if (!nextStatuses(appeal.getStatus()).contains(next))
            throw new BusinessException("Переход из " + appeal.getStatus() + " в " + next + " запрещён.");
        if (next == AppealStatus.IN_PROGRESS && appeal.getOperatorId() == null)
            throw new BusinessException("Нельзя взять обращение в работу без оператора.");
        if (next == AppealStatus.CLOSED && appeal.getOperatorId() == null)
            throw new BusinessException("Нельзя закрыть обращение без назначенного оператора.");
        appeal.setStatus(next);
        if (next == AppealStatus.CLOSED) appeal.setClosedAt(LocalDateTime.now());
        return repository.save(appeal);
    }

    public void delete(int id) throws SQLException {
        if (!repository.deleteById(id))
            throw new EntityNotFoundException("Обращение с ID " + id + " не найдено.");
    }

    // ---------- Поиск, фильтры, сортировка для UI ----------

    public List<Appeal> find(AppealFilter f) throws SQLException {
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to()))
            throw new BusinessException("Поле «Дата от» не может быть позже поля «Дата до».");
        String q = f.text() == null ? "" : f.text().trim().toLowerCase();
        return getAll().stream()
                .filter(a -> q.isEmpty() || matchesText(a, q))
                .filter(a -> f.status() == null || a.getStatus() == f.status())
                .filter(a -> f.category() == null || a.getCategory().equals(f.category()))
                .filter(a -> f.from() == null || !a.getCreatedAt().toLocalDate().isBefore(f.from()))
                .filter(a -> f.to() == null || !a.getCreatedAt().toLocalDate().isAfter(f.to()))
                .sorted(comparator(f.sort()))
                .toList();
    }

    public AppealDetails getDetails(int id) throws SQLException {
        Appeal appeal = getById(id);
        Client client = clientService.getById(appeal.getClientId());
        Operator operator = appeal.getOperatorId() == null ? null : operatorService.getById(appeal.getOperatorId());
        return new AppealDetails(appeal, client, operator);
    }

    public AppealStatistics statistics(List<Appeal> appeals) { return AppealStatistics.of(appeals); }

    /** Статусы, в которые можно перейти из текущего (для выбора в диалоге). */
    public List<AppealStatus> nextStatuses(AppealStatus from) {
        return switch (from) {
            case NEW -> List.of(AppealStatus.IN_PROGRESS);
            case IN_PROGRESS -> List.of(AppealStatus.ESCALATED, AppealStatus.RESOLVED);
            case ESCALATED -> List.of(AppealStatus.RESOLVED);
            case RESOLVED -> List.of(AppealStatus.CLOSED);
            case CLOSED -> List.of();
        };
    }

    // ---------- Валидация ----------

    private void validateFields(Appeal a) throws SQLException {
        if (a.getClientId() == null) throw new BusinessException("client", "Поле «Клиент»: выберите клиента из списка.");
        if (blank(a.getTopic())) throw new BusinessException("topic", "Поле «Тема»: введите тему обращения.");
        int length = a.getTopic().trim().length();
        if (length > MAX_TOPIC_LENGTH)
            throw new BusinessException("topic", "Поле «Тема»: не более " + MAX_TOPIC_LENGTH + " символов (сейчас " + length + ").");
        if (a.getCategory() == null || !CATEGORIES.contains(a.getCategory()))
            throw new BusinessException("category", "Поле «Категория»: выберите категорию из списка.");
        a.setTopic(a.getTopic().trim());
        Client client = clientService.getById(a.getClientId());
        a.setClientName(client.getFullName());
        a.setClientPhone(client.getPhone());
    }

    private void validateAssignment(Appeal a, Appeal previous) throws SQLException {
        if (a.getStatus() != AppealStatus.NEW && a.getOperatorId() == null)
            throw new BusinessException("operator", "Поле «Оператор»: для обращения в работе или завершённого нужен оператор.");
        if (a.getOperatorId() == null) return;
        Operator operator = operatorService.getById(a.getOperatorId());
        boolean unchanged = previous != null && Objects.equals(previous.getOperatorId(), a.getOperatorId());
        if (!unchanged && !operator.isActive())
            throw new BusinessException("operator", "Поле «Оператор»: нельзя назначить неактивного оператора, выберите другого.");
        if (!unchanged && isActive(a) && repository.countActiveByOperator(a.getOperatorId()) >= MAX_ACTIVE_PER_OPERATOR)
            throw new BusinessException("operator", "Поле «Оператор»: у оператора уже " + MAX_ACTIVE_PER_OPERATOR + " активных обращений, выберите другого.");
    }

    // ---------- Вспомогательное ----------

    private boolean matchesText(Appeal a, String q) {
        return a.getClientName().toLowerCase().contains(q)
                || a.getClientPhone().contains(q)
                || a.getTopic().toLowerCase().contains(q);
    }

    private Comparator<Appeal> comparator(AppealFilter.SortField sort) {
        return switch (sort == null ? AppealFilter.SortField.ID : sort) {
            case ID -> Comparator.comparing(Appeal::getId);
            case CREATED_NEWEST -> Comparator.comparing(Appeal::getCreatedAt).reversed();
            case CREATED_OLDEST -> Comparator.comparing(Appeal::getCreatedAt);
            case STATUS -> Comparator.comparing(Appeal::getStatus).thenComparing(Appeal::getCreatedAt);
        };
    }

    private boolean isActive(Appeal a) {
        return a.getStatus() == AppealStatus.NEW || a.getStatus() == AppealStatus.IN_PROGRESS || a.getStatus() == AppealStatus.ESCALATED;
    }

    private boolean blank(String text) { return text == null || text.isBlank(); }
}
