package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.*;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.repository.OperatorRepository;
import java.sql.SQLException;
import java.util.List;

public class OperatorService {
    private final OperatorRepository repository;
    public OperatorService(OperatorRepository repository) { this.repository=repository; }
    public List<Operator> getAll() throws SQLException { return repository.findAll(); }
    public Operator getById(int id) throws SQLException { return repository.findById(id).orElseThrow(()->new EntityNotFoundException("Оператор с ID " + id + " не найден.")); }
    public Operator create(Operator operator) throws SQLException { validate(operator); return repository.save(operator); }
    public Operator update(Operator operator) throws SQLException { getById(operator.getId()); validate(operator); return repository.save(operator); }
    public void delete(int id) throws SQLException { if(!repository.deleteById(id)) throw new EntityNotFoundException("Оператор с ID " + id + " не найден."); }
    private void validate(Operator o) { if(blank(o.getFullName())||blank(o.getLogin())||blank(o.getDepartment())) throw new BusinessException("ФИО, логин и отдел обязательны."); }
    private boolean blank(String text) { return text==null||text.isBlank(); }
}
