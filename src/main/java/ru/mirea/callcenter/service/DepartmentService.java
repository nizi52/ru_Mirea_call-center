package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.Department;
import ru.mirea.callcenter.repository.DepartmentRepository;
import ru.mirea.callcenter.repository.Repository;

import java.sql.SQLException;
import java.util.List;

public class DepartmentService {
    private final Repository<Department, Integer> repository;

    public DepartmentService(DepartmentRepository repository) { this.repository = repository; }
    public List<Department> getAll() throws SQLException { return repository.findAll(); }
    public Department getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Отдел с ID " + id + " не найден."));
    }
    public Department create(Department department) throws SQLException { validate(department); return repository.save(department); }
    public Department update(Department department) throws SQLException { getById(department.getId()); validate(department); return repository.save(department); }
    public void delete(int id) throws SQLException {
        getById(id);
        if (!repository.deleteById(id)) throw new EntityNotFoundException("Отдел с ID " + id + " не найден.");
    }
    private void validate(Department department) {
        if (department.getName() == null || department.getName().isBlank())
            throw new BusinessException("Название отдела обязательно.");
    }
}
