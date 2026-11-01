package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.service.AppealDetails;
import ru.mirea.callcenter.ui.util.Labels;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DetailsController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Label topicValue, categoryValue, statusValue, createdValue, closedValue;
    @FXML private Label clientNameValue, clientPhoneValue, clientEmailValue;
    @FXML private Label operatorNameValue, operatorLoginValue, operatorDepartmentValue, operatorActiveValue;

    public void setDetails(AppealDetails details) {
        Appeal a = details.appeal();
        topicValue.setText(a.getTopic());
        categoryValue.setText(Labels.category(a.getCategory()));
        statusValue.setText(Labels.status(a.getStatus()));
        createdValue.setText(format(a.getCreatedAt()));
        closedValue.setText(format(a.getClosedAt()));

        Client c = details.client();
        clientNameValue.setText(c.getFullName());
        clientPhoneValue.setText(c.getPhone());
        clientEmailValue.setText(c.getEmail() == null || c.getEmail().isBlank() ? "—" : c.getEmail());

        Operator o = details.operator();
        if (o == null) {
            operatorNameValue.setText("не назначен");
            operatorLoginValue.setText("—");
            operatorDepartmentValue.setText("—");
            operatorActiveValue.setText("—");
        } else {
            operatorNameValue.setText(o.getFullName());
            operatorLoginValue.setText(o.getLogin());
            operatorDepartmentValue.setText(o.getDepartmentName());
            operatorActiveValue.setText(o.isActive() ? "активен" : "неактивен");
        }
    }

    @FXML
    private void onClose() { ((Stage) topicValue.getScene().getWindow()).close(); }

    private String format(LocalDateTime time) { return time == null ? "—" : DATE_TIME.format(time); }
}
