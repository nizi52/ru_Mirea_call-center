package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.ui.AppContext;

import java.util.Map;

public class CallCenterFormController extends EntityFormController<CallCenter> {
    @FXML private TextField nameField, addressField;
    @FXML private CheckBox activeCheck;

    @FXML
    private void initialize() { activeCheck.setSelected(true); }

    @Override
    protected Map<String, Control> fields() { return Map.of("name", nameField, "address", addressField); }

    @Override protected String createTitle() { return "Новый колл-центр"; }
    @Override protected String editTitle() { return "Редактирование колл-центра"; }

    @Override
    protected void fill(CallCenter c) {
        nameField.setText(c.getName());
        addressField.setText(c.getAddress());
        activeCheck.setSelected(c.isActive());
    }

    @Override
    protected CallCenter build() {
        return new CallCenter(editing == null ? null : editing.getId(),
                nameField.getText(), addressField.getText(), activeCheck.isSelected());
    }

    @Override
    protected void save(CallCenter c) throws Exception {
        if (c.getId() == null) AppContext.callCenters().create(c);
        else AppContext.callCenters().update(c);
    }
}
