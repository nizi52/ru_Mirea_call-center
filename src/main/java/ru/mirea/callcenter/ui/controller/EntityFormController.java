package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.ui.util.UiErrors;

import java.util.Map;

/**
 * Общая логика формы добавления/редактирования: заполнение полей, сохранение,
 * показ ошибки и подсветка неверного поля красной рамкой.
 */
public abstract class EntityFormController<T> {
    private static final String ERROR_STYLE = "field-error";

    @FXML protected Label titleLabel;
    @FXML protected Label errorLabel;

    /** null — создаём новую запись, иначе редактируем эту. */
    protected T editing;
    private boolean saved;

    /** Код поля из BusinessException → элемент формы, который надо подсветить. */
    protected abstract Map<String, Control> fields();
    protected abstract String createTitle();
    protected abstract String editTitle();
    protected abstract void fill(T item);
    protected abstract T build();
    protected abstract void save(T item) throws Exception;

    public void setEntity(T item) {
        editing = item;
        titleLabel.setText(item == null ? createTitle() : editTitle());
        if (item != null) fill(item);
    }

    public boolean isSaved() { return saved; }

    @FXML
    protected void onSave() {
        clearErrors();
        try {
            save(build());
            saved = true;
            close();
        } catch (BusinessException e) {
            showError(e.getField(), e.getMessage());
        } catch (Exception e) {
            showError(null, UiErrors.message(e));
        }
    }

    @FXML
    protected void onCancel() { close(); }

    protected void showError(String field, String message) {
        errorLabel.setText(message);
        Control control = field == null ? null : fields().get(field);
        if (control != null) {
            control.getStyleClass().add(ERROR_STYLE);
            control.requestFocus();
        }
    }

    private void clearErrors() {
        errorLabel.setText("");
        fields().values().forEach(c -> c.getStyleClass().remove(ERROR_STYLE));
    }

    private void close() { ((Stage) errorLabel.getScene().getWindow()).close(); }
}
