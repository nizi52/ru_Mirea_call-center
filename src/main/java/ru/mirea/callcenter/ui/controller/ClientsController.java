package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;
import java.util.Map;

public class ClientsController extends EntityListController<Client> {
    @FXML private TableColumn<Client, String> nameColumn, phoneColumn, emailColumn;
    @FXML private TableColumn<Client, Number> appealsColumn;

    /** Сколько обращений у каждого клиента (client_id → количество). */
    private Map<Integer, Long> appealCounts = Map.of();

    @Override
    protected void setupColumns() {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        phoneColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPhone()));
        emailColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getEmail() == null ? "—" : c.getValue().getEmail()));
        appealsColumn.setCellValueFactory(c -> new ReadOnlyLongWrapper(appealCounts.getOrDefault(c.getValue().getId(), 0L)));
    }

    @Override
    protected List<Client> load() throws Exception {
        appealCounts = AppContext.appeals().countByClient();
        return AppContext.clients().getAll();
    }

    @Override
    protected boolean matches(Client c, String q) {
        return contains(c.getFullName(), q) || contains(c.getPhone(), q) || contains(c.getEmail(), q);
    }

    @Override protected Integer idOf(Client c) { return c.getId(); }
    @Override protected String countCaption() { return "Показано клиентов"; }

    @Override
    protected boolean openForm(Client client) throws Exception {
        Client fresh = client == null ? null : AppContext.clients().getById(client.getId());
        return WindowFactory.<ClientFormController>openModal(owner(), "client-form.fxml",
                client == null ? "Новый клиент" : "Редактирование клиента", c -> c.setEntity(fresh)).isSaved();
    }

    @Override
    protected String deleteQuestion(Client c) { return "Удалить клиента «" + c.getFullName() + "»?"; }

    @Override
    protected void delete(Client c) throws Exception { AppContext.clients().delete(c.getId()); }
}
