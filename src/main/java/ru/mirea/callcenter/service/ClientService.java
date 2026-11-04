package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.repository.ClientRepository;

import java.sql.SQLException;
import java.util.List;

public class ClientService {
    private final ClientRepository repository;

    public ClientService(ClientRepository repository) { this.repository = repository; }

    public List<Client> getAll() throws SQLException { return repository.findAll(); }

    /** Поиск по ФИО, телефону и e-mail (без учёта регистра). Пустой запрос — все клиенты. */
    public List<Client> search(String query) throws SQLException {
        String q = Text.normalize(query);
        return repository.findAll().stream()
                .filter(c -> q.isEmpty() || Text.contains(c.getFullName(), q)
                        || Text.contains(c.getPhone(), q) || Text.contains(c.getEmail(), q))
                .toList();
    }

    public Client getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден."));
    }

    public Client create(Client client) throws SQLException { validate(client); return repository.save(client); }

    public Client update(Client client) throws SQLException { getById(client.getId()); validate(client); return repository.save(client); }

    public void delete(int id) throws SQLException {
        getById(id);
        int appeals = repository.countAppeals(id);
        if (appeals > 0)
            throw new BusinessException("Нельзя удалить клиента: с ним связаны обращения (" + appeals + "). Сначала удалите их.");
        if (!repository.deleteById(id)) throw new EntityNotFoundException("Клиент с ID " + id + " не найден.");
    }

    private void validate(Client client) throws SQLException {
        String name = Text.trim(client.getFullName());
        String phone = Text.trim(client.getPhone());
        String email = Text.trim(client.getEmail());
        if (name.isEmpty()) throw new BusinessException("fullName", "Укажите ФИО клиента.");
        if (name.length() > 150) throw new BusinessException("fullName", "ФИО не длиннее 150 символов.");
        if (phone.isEmpty()) throw new BusinessException("phone", "Укажите телефон клиента.");
        if (!phone.matches("\\+?[0-9 ()-]{5,30}"))
            throw new BusinessException("phone", "Телефон: только цифры, пробелы, скобки, дефис и «+» в начале (5–30 символов).");
        if (!email.isEmpty() && (email.length() > 150 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")))
            throw new BusinessException("email", "Некорректный e-mail.");
        boolean phoneTaken = repository.findAll().stream()
                .anyMatch(c -> c.getPhone().trim().equals(phone) && !c.getId().equals(client.getId()));
        if (phoneTaken) throw new BusinessException("phone", "Клиент с таким телефоном уже есть.");
        client.setFullName(name);
        client.setPhone(phone);
        client.setEmail(email.isEmpty() ? null : email);
    }
}
