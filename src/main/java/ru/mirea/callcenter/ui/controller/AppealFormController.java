package ru.mirea.callcenter.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.Client;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.service.AppealService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.Labels;
import ru.mirea.callcenter.ui.util.UiErrors;

import java.util.Map;
import java.util.function.Function;

public class AppealFormController {
    private static final int MAX_TOPIC = 300;
    private static final String ERROR_STYLE = "field-error";

    private final AppealService appealService = AppContext.appeals();
    private Appeal editing;
    private boolean saved;

    @FXML private Label titleLabel;
    @FXML private ComboBox<Client> clientBox;
    @FXML private ComboBox<Operator> operatorBox;
    @FXML private ComboBox<String> categoryBox;
    @FXML private TextArea topicArea;
    @FXML private Label counterLabel;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private Map<String, Control> fields;

    @FXML
    private void initialize() {
        fields = Map.of("client", clientBox, "operator", operatorBox, "category", categoryBox, "topic", topicArea);
        clientBox.setConverter(converter(c -> c.getFullName() + ", " + c.getPhone() + (c.isActive() ? "" : " (неактивен)")));
        operatorBox.setConverter(converter(o -> o.getFullName() + " (" + o.getCallCenterName() + ")"));
        categoryBox.setConverter(converter(Labels::category));
        categoryBox.setItems(FXCollections.observableArrayList(AppealService.categories()));
        topicArea.textProperty().addListener((obs, old, now) ->
                counterLabel.setText(now.trim().length() + " / " + MAX_TOPIC));
        try {
            clientBox.setItems(FXCollections.observableArrayList(AppContext.clients().getActive()));
            operatorBox.setItems(FXCollections.observableArrayList(AppContext.operators().getActive()));
        } catch (Exception e) {
            showError(null, UiErrors.message(e));
            saveButton.setDisable(true);
        }
    }

    /** null — создание нового обращения, иначе редактирование. */
    public void setAppeal(Appeal appeal) {
        this.editing = appeal;
        titleLabel.setText(appeal == null ? "Новое обращение" : "Редактирование обращения");
        if (appeal == null) return;
        selectClient(appeal.getClientId());
        selectOperator(appeal.getOperatorId());
        categoryBox.setValue(appeal.getCategory());
        topicArea.setText(appeal.getTopic());
    }

    private void selectClient(Integer clientId) {
        try {
            // клиент мог стать неактивным — показываем его, чтобы старое обращение можно было править
            Client current = clientBox.getItems().stream().filter(c -> c.getId().equals(clientId))
                    .findFirst().orElse(null);
            if (current == null) {
                current = AppContext.clients().getById(clientId);
                clientBox.getItems().add(current);
            }
            clientBox.setValue(current);
        } catch (Exception e) {
            showError(null, UiErrors.message(e));
        }
    }

    private void selectOperator(Integer operatorId) {
        if (operatorId == null) return;
        try {
            // оператор мог стать неактивным — всё равно показываем его, иначе потеряем назначение
            Operator current = operatorBox.getItems().stream().filter(o -> o.getId().equals(operatorId))
                    .findFirst().orElse(null);
            if (current == null) {
                current = AppContext.operators().getById(operatorId);
                operatorBox.getItems().add(current);
            }
            operatorBox.setValue(current);
        } catch (Exception e) {
            showError(null, UiErrors.message(e));
        }
    }

    public boolean isSaved() { return saved; }

    @FXML
    private void onClearOperator() { operatorBox.setValue(null); }

    @FXML
    private void onSave() {
        clearErrors();
        Client client = clientBox.getValue();
        Operator operator = operatorBox.getValue();
        Appeal appeal = new Appeal(editing == null ? null : editing.getId(),
                operator == null ? null : operator.getId(),
                client == null ? null : client.getId(), null, null,
                topicArea.getText(), categoryBox.getValue(), null, null, null);
        try {
            if (editing == null) appealService.create(appeal);
            else appealService.updateDetails(appeal);
            saved = true;
            close();
        } catch (BusinessException e) {
            showError(e.getField(), e.getMessage());
        } catch (Exception e) {
            showError(null, UiErrors.message(e));
        }
    }

    @FXML
    private void onCancel() { close(); }

    private void showError(String field, String message) {
        errorLabel.setText(message);
        Control control = field == null ? null : fields.get(field);
        if (control != null) {
            control.getStyleClass().add(ERROR_STYLE);
            control.requestFocus();
        }
    }

    private void clearErrors() {
        errorLabel.setText("");
        fields.values().forEach(c -> c.getStyleClass().remove(ERROR_STYLE));
    }

    private void close() { ((Stage) errorLabel.getScene().getWindow()).close(); }

    private <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : toText.apply(value); }
            @Override public T fromString(String text) { return null; }
        };
    }
}
