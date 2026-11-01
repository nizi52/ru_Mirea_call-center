package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.repository.ClientRepository;
import ru.mirea.callcenter.repository.Repository;
import ru.mirea.callcenter.util.DbErrors;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class ClientService {
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{10,15}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final Repository<Client, Integer> repository;

    public ClientService(ClientRepository repository) { this.repository = repository; }
    public List<Client> getAll() throws SQLException { return repository.findAll(); }
    public Client getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден."));
    }
    public Client create(Client client) throws SQLException { validate(client); return save(client); }
    public Client update(Client client) throws SQLException { getById(client.getId()); validate(client); return save(client); }
    public void delete(int id) throws SQLException {
        getById(id);
        try {
            if (!repository.deleteById(id)) throw new EntityNotFoundException("Клиент с ID " + id + " не найден.");
        } catch (SQLException e) {
            if (DbErrors.isForeignKeyViolation(e))
                throw new BusinessException("Нельзя удалить клиента: у него есть обращения. Сначала удалите их.");
            throw e;
        }
    }

    private Client save(Client client) throws SQLException {
        try {
            return repository.save(client);
        } catch (SQLException e) {
            if (DbErrors.isUniqueViolation(e))
                throw new BusinessException("phone", "Поле «Телефон»: клиент с таким номером уже есть.");
            throw e;
        }
    }

    /** Проверяет поля и приводит их к единому виду (обрезает пробелы, пустой email → null). */
    private void validate(Client c) {
        if (blank(c.getFullName())) throw new BusinessException("fullName", "Поле «ФИО»: введите имя клиента.");
        if (c.getFullName().trim().length() > 150) throw new BusinessException("fullName", "Поле «ФИО»: не более 150 символов.");
        if (blank(c.getPhone())) throw new BusinessException("phone", "Поле «Телефон»: введите номер.");
        String phone = c.getPhone().replaceAll("[\\s()-]", "");
        if (!PHONE.matcher(phone).matches())
            throw new BusinessException("phone", "Поле «Телефон»: от 10 до 15 цифр, можно с «+» в начале.");
        String email = blank(c.getEmail()) ? null : c.getEmail().trim();
        if (email != null && !EMAIL.matcher(email).matches())
            throw new BusinessException("email", "Поле «Email»: неверный формат, пример: name@mail.ru.");
        c.setFullName(c.getFullName().trim());
        c.setPhone(phone);
        c.setEmail(email);
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
