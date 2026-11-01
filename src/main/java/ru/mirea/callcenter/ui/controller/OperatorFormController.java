package ru.mirea.callcenter.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.Converters;
import ru.mirea.callcenter.ui.util.UiErrors;

import java.util.Map;

public class OperatorFormController extends EntityFormController<Operator> {
    @FXML private TextField fullNameField, loginField;
    @FXML private ComboBox<CallCenter> callCenterBox;
    @FXML private CheckBox activeCheck;
    @FXML private Button saveButton;

    @FXML
    private void initialize() {
        callCenterBox.setConverter(Converters.of(c -> c.getName() + (c.isActive() ? "" : " (неактивен)")));
        activeCheck.setSelected(true);
        try {
            callCenterBox.setItems(FXCollections.observableArrayList(AppContext.callCenters().getActive()));
        } catch (Exception e) {
            showError(null, UiErrors.message(e));
            saveButton.setDisable(true);
        }
    }

    @Override
    protected Map<String, Control> fields() {
        return Map.of("fullName", fullNameField, "login", loginField, "callCenter", callCenterBox);
    }

    @Override protected String createTitle() { return "Новый оператор"; }
    @Override protected String editTitle() { return "Редактирование оператора"; }

    @Override
    protected void fill(Operator o) {
        fullNameField.setText(o.getFullName());
        loginField.setText(o.getLogin());
        activeCheck.setSelected(o.isActive());
        // колл-центр мог стать неактивным — всё равно показываем его, чтобы не потерять привязку
        CallCenter current = callCenterBox.getItems().stream()
                .filter(c -> c.getId().equals(o.getCallCenterId())).findFirst().orElse(null);
        if (current == null) {
            try {
                current = AppContext.callCenters().getById(o.getCallCenterId());
                callCenterBox.getItems().add(current);
            } catch (Exception e) {
                showError(null, UiErrors.message(e));
            }
        }
        callCenterBox.setValue(current);
    }

    @Override
    protected Operator build() {
        CallCenter cc = callCenterBox.getValue();
        return new Operator(editing == null ? null : editing.getId(), fullNameField.getText(), loginField.getText(),
                cc == null ? null : cc.getId(), null, activeCheck.isSelected());
    }

    @Override
    protected void save(Operator o) throws Exception {
        if (o.getId() == null) AppContext.operators().create(o);
        else AppContext.operators().update(o);
    }
}
