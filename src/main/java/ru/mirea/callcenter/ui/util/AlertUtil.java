package ru.mirea.callcenter.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class AlertUtil {
    private AlertUtil() { }

    @FunctionalInterface
    public interface ThrowingAction { void run() throws Exception; }

    public static void info(String title, String message) { show(Alert.AlertType.INFORMATION, title, message); }

    public static void error(String message) { show(Alert.AlertType.ERROR, "Ошибка", message); }

    public static boolean confirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(header);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    /** Выполняет действие; любую ошибку показывает пользователю понятным сообщением. */
    public static boolean run(ThrowingAction action) {
        try {
            action.run();
            return true;
        } catch (Exception e) {
            error(UiErrors.message(e));
            return false;
        }
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
