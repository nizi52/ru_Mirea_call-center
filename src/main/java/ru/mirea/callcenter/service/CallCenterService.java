package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.repository.CallCenterRepository;
import ru.mirea.callcenter.repository.Repository;
import ru.mirea.callcenter.util.DbErrors;

import java.sql.SQLException;
import java.util.List;

public class CallCenterService {
    private final Repository<CallCenter, Integer> repository;

    public CallCenterService(CallCenterRepository repository) { this.repository = repository; }
    public List<CallCenter> getAll() throws SQLException { return repository.findAll(); }
    public List<CallCenter> getActive() throws SQLException { return getAll().stream().filter(CallCenter::isActive).toList(); }
    public CallCenter getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Колл-центр с ID " + id + " не найден."));
    }
    public CallCenter create(CallCenter callCenter) throws SQLException { validate(callCenter); return save(callCenter); }
    public CallCenter update(CallCenter callCenter) throws SQLException { getById(callCenter.getId()); validate(callCenter); return save(callCenter); }
    public void delete(int id) throws SQLException {
        getById(id);
        try {
            if (!repository.deleteById(id)) throw new EntityNotFoundException("Колл-центр с ID " + id + " не найден.");
        } catch (SQLException e) {
            if (DbErrors.isForeignKeyViolation(e))
                throw new BusinessException("Нельзя удалить колл-центр: в нём есть операторы. Переведите их в другой колл-центр или сделайте колл-центр неактивным.");
            throw e;
        }
    }

    private CallCenter save(CallCenter callCenter) throws SQLException {
        try {
            return repository.save(callCenter);
        } catch (SQLException e) {
            if (DbErrors.isUniqueViolation(e))
                throw new BusinessException("name", "Поле «Название»: колл-центр с таким названием уже есть.");
            throw e;
        }
    }

    private void validate(CallCenter c) {
        if (blank(c.getName())) throw new BusinessException("name", "Поле «Название»: введите название колл-центра.");
        if (c.getName().trim().length() > 80) throw new BusinessException("name", "Поле «Название»: не более 80 символов.");
        if (blank(c.getAddress())) throw new BusinessException("address", "Поле «Адрес»: введите адрес.");
        if (c.getAddress().trim().length() > 200) throw new BusinessException("address", "Поле «Адрес»: не более 200 символов.");
        c.setName(c.getName().trim());
        c.setAddress(c.getAddress().trim());
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
