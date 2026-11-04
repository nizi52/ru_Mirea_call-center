package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.service.ClientService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;

public class ClientsController extends CrudTabController<Client> {
    private final ClientService clientService = AppContext.clients();

    @FXML private TableColumn<Client, Integer> idColumn;
    @FXML private TableColumn<Client, String> nameColumn;
    @FXML private TableColumn<Client, String> phoneColumn;
    @FXML private TableColumn<Client, String> emailColumn;

    @Override
    protected void setupColumns() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        phoneColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPhone()));
        emailColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEmail()));
    }

    @Override
    protected List<Client> load(String query) throws Exception { return clientService.search(query); }

    @Override
    protected boolean openForm(Client editing) throws Exception {
        ClientFormController form = WindowFactory.openModal(window(), "client-form.fxml",
                editing == null ? "Новый клиент" : "Редактирование клиента", f -> f.setClient(editing));
        return form.getSaved() != null;
    }

    @Override
    protected void delete(Client item) throws Exception { clientService.delete(item.getId()); }

    @Override
    protected String describe(Client item) { return "клиента «" + item.getFullName() + "»"; }

    @Override
    protected String countText(int count) { return "Клиентов: " + count; }
}
