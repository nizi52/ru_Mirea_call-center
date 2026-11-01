package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.repository.OperatorRepository;
import ru.mirea.callcenter.repository.Repository;
import ru.mirea.callcenter.util.DbErrors;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class OperatorService {
    private static final Pattern LOGIN = Pattern.compile("[A-Za-z0-9_.]{3,50}");

    private final Repository<Operator, Integer> repository;
    private final CallCenterService callCenters;

    public OperatorService(OperatorRepository repository, CallCenterService callCenters) {
        this.repository = repository;
        this.callCenters = callCenters;
    }
    public List<Operator> getAll() throws SQLException { return repository.findAll(); }
    public List<Operator> getActive() throws SQLException { return repository.findAll().stream().filter(Operator::isActive).toList(); }
    public Operator getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Оператор с ID " + id + " не найден."));
    }
    public Operator create(Operator operator) throws SQLException { validate(operator, null); return save(operator); }
    public Operator update(Operator operator) throws SQLException {
        Operator old = getById(operator.getId());
        validate(operator, old);
        return save(operator);
    }
    /**
     * Новые обращения удалённого оператора остаются, но становятся неназначенными (ON DELETE SET NULL).
     * Если у оператора есть обращения в работе или завершённые, БД не даст убрать оператора
     * (CHECK working_requires_operator): такие обращения без оператора существовать не могут.
     */
    public void delete(int id) throws SQLException {
        getById(id);
        try {
            if (!repository.deleteById(id)) throw new EntityNotFoundException("Оператор с ID " + id + " не найден.");
        } catch (SQLException e) {
            if (DbErrors.isCheckViolation(e))
                throw new BusinessException("Нельзя удалить оператора: за ним есть обращения в работе или завершённые. "
                        + "Снимите галочку «Активен», чтобы ему больше не назначали новые обращения.");
            throw e;
        }
    }

    private Operator save(Operator operator) throws SQLException {
        try {
            return repository.save(operator);
        } catch (SQLException e) {
            if (DbErrors.isUniqueViolation(e))
                throw new BusinessException("login", "Поле «Логин»: такой логин уже занят.");
            throw e;
        }
    }

    private void validate(Operator o, Operator old) throws SQLException {
        if (blank(o.getFullName())) throw new BusinessException("fullName", "Поле «ФИО»: введите имя оператора.");
        if (o.getFullName().trim().length() > 150) throw new BusinessException("fullName", "Поле «ФИО»: не более 150 символов.");
        if (blank(o.getLogin())) throw new BusinessException("login", "Поле «Логин»: введите логин.");
        if (!LOGIN.matcher(o.getLogin().trim()).matches())
            throw new BusinessException("login", "Поле «Логин»: от 3 до 50 символов, латиница, цифры, «_» и «.».");
        if (o.getCallCenterId() == null) throw new BusinessException("callCenter", "Поле «Колл-центр»: выберите колл-центр из списка.");
        CallCenter callCenter = callCenters.getById(o.getCallCenterId());
        // в неактивный колл-центр нельзя переводить; если оператор уже там, редактировать его можно
        boolean sameCallCenter = old != null && Objects.equals(old.getCallCenterId(), o.getCallCenterId());
        if (!callCenter.isActive() && !sameCallCenter)
            throw new BusinessException("callCenter", "Поле «Колл-центр»: нельзя назначить оператора в неактивный колл-центр.");
        o.setFullName(o.getFullName().trim());
        o.setLogin(o.getLogin().trim());
        o.setCallCenterName(callCenter.getName());
    }

    private boolean blank(String text) { return text == null || text.isBlank(); }
}
