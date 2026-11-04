package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.service.CallCenterService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.FormErrors;

import java.util.Map;

public class CallCenterFormController {
    private final CallCenterService callCenterService = AppContext.callCenters();
    private CallCenter editing;
    private boolean saved;

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private CheckBox activeBox;
    @FXML private Label errorLabel;

    private FormErrors errors;

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel, Map.of("name", nameField, "address", addressField));
        titleLabel.setText("Новый колл-центр");
    }

    /** null — создание нового колл-центра, иначе редактирование. */
    public void setCallCenter(CallCenter callCenter) {
        this.editing = callCenter;
        if (callCenter == null) return;
        titleLabel.setText("Редактирование колл-центра");
        nameField.setText(callCenter.getName());
        addressField.setText(callCenter.getAddress());
        activeBox.setSelected(callCenter.isActive());
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void onSave() {
        errors.clear();
        CallCenter callCenter = new CallCenter(editing == null ? null : editing.getId(),
                nameField.getText(), addressField.getText(), activeBox.isSelected());
        try {
            if (editing == null) callCenterService.create(callCenter);
            else callCenterService.update(callCenter);
            saved = true;
            close();
        } catch (Exception e) {
            errors.handle(e);
        }
    }

    @FXML
    private void onCancel() { close(); }

    private void close() { ((Stage) errorLabel.getScene().getWindow()).close(); }
}
