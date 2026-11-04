package ru.mirea.callcenter.ui.util;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import ru.mirea.callcenter.exception.BusinessException;

import java.util.Map;

/** Показ ошибок в форме: текст под полями и красная рамка у поля, на которое указал сервис. */
public final class FormErrors {
    private static final String ERROR_STYLE = "field-error";

    private final Label errorLabel;
    private final Map<String, Control> fields;

    public FormErrors(Label errorLabel, Map<String, Control> fields) {
        this.errorLabel = errorLabel;
        this.fields = fields;
    }

    public void handle(Exception e) {
        if (e instanceof BusinessException b) show(b.getField(), b.getMessage());
        else show(null, UiErrors.message(e));
    }

    public void show(String field, String message) {
        errorLabel.setText(message);
        Control control = field == null ? null : fields.get(field);
        if (control != null) {
            control.getStyleClass().add(ERROR_STYLE);
            control.requestFocus();
        }
    }

    public void clear() {
        errorLabel.setText("");
        fields.values().forEach(c -> c.getStyleClass().remove(ERROR_STYLE));
    }
}
