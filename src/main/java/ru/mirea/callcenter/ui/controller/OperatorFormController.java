package ru.mirea.callcenter.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.service.OperatorService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.FormErrors;
import ru.mirea.callcenter.ui.util.UiErrors;

import java.util.Map;

public class OperatorFormController {
    private final OperatorService operatorService = AppContext.operators();
    private Operator editing;
    private boolean saved;

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField loginField;
    @FXML private ComboBox<CallCenter> callCenterBox;
    @FXML private CheckBox activeBox;
    @FXML private Label errorLabel;

    private FormErrors errors;

    @FXML
    private void initialize() {
        errors = new FormErrors(errorLabel, Map.of("fullName", nameField, "login", loginField, "callCenter", callCenterBox));
        titleLabel.setText("Новый оператор");
        callCenterBox.setConverter(new StringConverter<>() {
            @Override public String toString(CallCenter d) {
                return d == null ? "" : d.getName() + (d.isActive() ? "" : " (неактивен)");
            }
            @Override public CallCenter fromString(String s) { return null; }
        });
        try {
            // назначить можно только в активный колл-центр
            callCenterBox.setItems(FXCollections.observableArrayList(AppContext.callCenters().getActive()));
        } catch (Exception e) {
            errors.show(null, UiErrors.message(e));
        }
    }

    /** null — создание нового оператора, иначе редактирование. */
    public void setOperator(Operator operator) {
        this.editing = operator;
        if (operator == null) return;
        titleLabel.setText("Редактирование оператора");
        nameField.setText(operator.getFullName());
        loginField.setText(operator.getLogin());
        activeBox.setSelected(operator.isActive());
        try {
            // текущий колл-центр мог стать неактивным — всё равно показываем его, чтобы не потерять
            CallCenter current = callCenterBox.getItems().stream()
                    .filter(d -> d.getId().equals(operator.getCallCenterId())).findFirst().orElse(null);
            if (current == null) {
                current = AppContext.callCenters().getById(operator.getCallCenterId());
                callCenterBox.getItems().add(current);
            }
            callCenterBox.setValue(current);
        } catch (Exception e) {
            errors.show(null, UiErrors.message(e));
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void onSave() {
        errors.clear();
        CallCenter callCenter = callCenterBox.getValue();
        Operator operator = new Operator(editing == null ? null : editing.getId(), nameField.getText(), loginField.getText(),
                callCenter == null ? null : callCenter.getId(), null, activeBox.isSelected());
        try {
            if (editing == null) operatorService.create(operator);
            else operatorService.update(operator);
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
