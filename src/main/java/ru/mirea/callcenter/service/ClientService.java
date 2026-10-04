package ru.mirea.callcenter.service;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.repository.ClientRepository;
import ru.mirea.callcenter.repository.Repository;

import java.sql.SQLException;
import java.util.List;

public class ClientService {
    private final Repository<Client, Integer> repository;

    public ClientService(ClientRepository repository) { this.repository = repository; }
    public List<Client> getAll() throws SQLException { return repository.findAll(); }
    public Client getById(int id) throws SQLException {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден."));
    }
    public Client create(Client client) throws SQLException { validate(client); return repository.save(client); }
    public Client update(Client client) throws SQLException { getById(client.getId()); validate(client); return repository.save(client); }
    public void delete(int id) throws SQLException {
        getById(id);
        if (!repository.deleteById(id)) throw new EntityNotFoundException("Клиент с ID " + id + " не найден.");
    }
    private void validate(Client client) {
        if (client.getFullName() == null || client.getFullName().isBlank() || client.getPhone() == null || client.getPhone().isBlank())
            throw new BusinessException("ФИО и телефон клиента обязательны.");
    }
}
