package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.service.ClientService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.FormErrors;

import java.util.Map;

/** Форма клиента: создание (в том числе прямо из формы обращения) и редактирование. */
public class ClientFormController {
    private final ClientService clientService = AppContext.clients();
    private Client editing;
    private Client saved;

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label errorLabel;

    private FormErrors errors;

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel, Map.of("fullName", nameField, "phone", phoneField, "email", emailField));
        titleLabel.setText("Новый клиент");
    }

    /** null — создание нового клиента, иначе редактирование. */
    public void setClient(Client client) {
        this.editing = client;
        if (client == null) return;
        titleLabel.setText("Редактирование клиента");
        nameField.setText(client.getFullName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
    }

    /** Сохранённый клиент или null, если окно закрыли без сохранения. */
    public Client getSaved() { return saved; }

    @FXML
    private void onSave() {
        errors.clear();
        Client client = new Client(editing == null ? null : editing.getId(),
                nameField.getText(), phoneField.getText(), emailField.getText());
        try {
            saved = editing == null ? clientService.create(client) : clientService.update(client);
            close();
        } catch (Exception e) {
            errors.handle(e);
        }
    }

    @FXML
    private void onCancel() { close(); }

    private void close() { ((Stage) errorLabel.getScene().getWindow()).close(); }
}
