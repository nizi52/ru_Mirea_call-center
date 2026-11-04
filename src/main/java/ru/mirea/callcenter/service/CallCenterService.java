package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.repository.CallCenterRepository;

import java.sql.SQLException;
import java.util.List;

public class CallCenterService {
    private final CallCenterRepository repository;

    public CallCenterService(CallCenterRepository repository) { this.repository = repository; }

    public List<CallCenter> getAll() throws SQLException { return repository.findAll(); }

    public List<CallCenter> getActive() throws SQLException { return repository.findAll().stream().filter(CallCenter::isActive).toList(); }

    /** Поиск по названию и адресу. Пустой запрос — все колл-центры. */
    public List<CallCenter> search(String query) throws SQLException {
        String q = Text.normalize(query);
        return repository.findAll().stream()
                .filter(d -> q.isEmpty() || Text.contains(d.getName(), q) || Text.contains(d.getAddress(), q))
                .toList();
    }

    public CallCenter getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Колл-центр с ID " + id + " не найден."));
    }

    public CallCenter create(CallCenter callCenter) throws SQLException { validate(callCenter); return repository.save(callCenter); }

    public CallCenter update(CallCenter callCenter) throws SQLException { getById(callCenter.getId()); validate(callCenter); return repository.save(callCenter); }

    public void delete(int id) throws SQLException {
        getById(id);
        int operators = repository.countOperators(id);
        if (operators > 0)
            throw new BusinessException("Нельзя удалить колл-центр: в нём числятся операторы (" + operators
                    + "). Переведите их в другой колл-центр или сделайте колл-центр неактивным.");
        if (!repository.deleteById(id)) throw new EntityNotFoundException("Колл-центр с ID " + id + " не найден.");
    }

    private void validate(CallCenter callCenter) throws SQLException {
        String name = Text.trim(callCenter.getName());
        String address = Text.trim(callCenter.getAddress());
        if (name.isEmpty()) throw new BusinessException("name", "Укажите название колл-центра.");
        if (name.length() > 80) throw new BusinessException("name", "Название не длиннее 80 символов.");
        if (address.isEmpty()) throw new BusinessException("address", "Укажите адрес колл-центра.");
        if (address.length() > 200) throw new BusinessException("address", "Адрес не длиннее 200 символов.");
        boolean nameTaken = repository.findAll().stream()
                .anyMatch(d -> d.getName().equalsIgnoreCase(name) && !d.getId().equals(callCenter.getId()));
        if (nameTaken) throw new BusinessException("name", "Колл-центр с таким названием уже есть.");
        callCenter.setName(name);
        callCenter.setAddress(address);
    }
}
