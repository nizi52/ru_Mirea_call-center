package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.ui.AppContext;

import java.util.Map;

public class ClientFormController extends EntityFormController<Client> {
    @FXML private TextField fullNameField, phoneField, emailField;
    @FXML private CheckBox activeCheck;

    @FXML
    private void initialize() { activeCheck.setSelected(true); }

    @Override
    protected Map<String, Control> fields() {
        return Map.of("fullName", fullNameField, "phone", phoneField, "email", emailField);
    }

    @Override protected String createTitle() { return "Новый клиент"; }
    @Override protected String editTitle() { return "Редактирование клиента"; }

    @Override
    protected void fill(Client c) {
        fullNameField.setText(c.getFullName());
        phoneField.setText(c.getPhone());
        emailField.setText(c.getEmail());
        activeCheck.setSelected(c.isActive());
    }

    @Override
    protected Client build() {
        return new Client(editing == null ? null : editing.getId(),
                fullNameField.getText(), phoneField.getText(), emailField.getText(), activeCheck.isSelected());
    }

    @Override
    protected void save(Client c) throws Exception {
        if (c.getId() == null) AppContext.clients().create(c);
        else AppContext.clients().update(c);
    }
}
