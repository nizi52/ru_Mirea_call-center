package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.repository.OperatorRepository;

import java.sql.SQLException;
import java.util.List;

public class OperatorService {
    private final OperatorRepository repository;
    private final CallCenterService callCenters;

    public OperatorService(OperatorRepository repository, CallCenterService callCenters) {
        this.repository = repository;
        this.callCenters = callCenters;
    }

    public List<Operator> getAll() throws SQLException { return repository.findAll(); }

    public List<Operator> getActive() throws SQLException { return repository.findAll().stream().filter(Operator::isActive).toList(); }

    /** Поиск по ФИО и логину + фильтр по колл-центру (null — любой) и активности. */
    public List<Operator> search(String query, Integer callCenterId, boolean onlyActive) throws SQLException {
        String q = Text.normalize(query);
        return repository.findAll().stream()
                .filter(o -> q.isEmpty() || Text.contains(o.getFullName(), q) || Text.contains(o.getLogin(), q))
                .filter(o -> callCenterId == null || callCenterId.equals(o.getCallCenterId()))
                .filter(o -> !onlyActive || o.isActive())
                .toList();
    }

    public Operator getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Оператор с ID " + id + " не найден."));
    }

    public Operator create(Operator operator) throws SQLException { validate(operator, null); return repository.save(operator); }

    public Operator update(Operator operator) throws SQLException {
        Operator previous = getById(operator.getId());
        validate(operator, previous);
        return repository.save(operator);
    }

    public void delete(int id) throws SQLException {
        getById(id);
        int working = repository.countWorkingAppeals(id);
        if (working > 0)
            throw new BusinessException("Нельзя удалить оператора: за ним обращения в работе (" + working
                    + "). Переназначьте их или сделайте оператора неактивным.");
        if (!repository.deleteById(id)) throw new EntityNotFoundException("Оператор с ID " + id + " не найден.");
    }

    private void validate(Operator o, Operator previous) throws SQLException {
        String name = Text.trim(o.getFullName());
        String login = Text.trim(o.getLogin());
        if (name.isEmpty()) throw new BusinessException("fullName", "Укажите ФИО оператора.");
        if (name.length() > 150) throw new BusinessException("fullName", "ФИО не длиннее 150 символов.");
        if (login.isEmpty()) throw new BusinessException("login", "Укажите логин оператора.");
        if (!login.matches("[A-Za-z0-9._-]{3,50}"))
            throw new BusinessException("login", "Логин: 3–50 символов, латиница, цифры, точка, дефис или подчёркивание.");
        if (o.getCallCenterId() == null) throw new BusinessException("callCenter", "Выберите колл-центр.");
        CallCenter callCenter = callCenters.getById(o.getCallCenterId());
        boolean callCenterChanged = previous == null || !previous.getCallCenterId().equals(o.getCallCenterId());
        if (!callCenter.isActive() && callCenterChanged)
            throw new BusinessException("callCenter", "Нельзя назначить оператора в неактивный колл-центр.");
        boolean loginTaken = repository.findAll().stream()
                .anyMatch(x -> x.getLogin().equalsIgnoreCase(login) && !x.getId().equals(o.getId()));
        if (loginTaken) throw new BusinessException("login", "Логин уже занят другим оператором.");
        o.setFullName(name);
        o.setLogin(login);
        o.setCallCenterName(callCenter.getName());
    }
}
