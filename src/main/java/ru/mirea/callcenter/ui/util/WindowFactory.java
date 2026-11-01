package ru.mirea.callcenter.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class WindowFactory {
    private static final String BASE = "/ru/mirea/callcenter/ui/";

    /** Открытые окна разделов: fxml → окно. Каждый раздел открывается в одном экземпляре. */
    private static final Map<String, Stage> OPEN_WINDOWS = new HashMap<>();
    /** Контроллеры, которые надо обновить после изменения данных. */
    private static final List<Refreshable> LISTENERS = new ArrayList<>();

    private WindowFactory() { }

    /** Загружает FXML, передаёт контроллер в init, показывает окно и ждёт его закрытия. */
    public static <T> T openModal(Window owner, String fxml, String title, Consumer<T> init) throws IOException {
        FXMLLoader loader = loader(fxml);
        Parent root = loader.load();
        T controller = loader.getController();
        if (init != null) init.accept(controller);
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(scene(root));
        stage.showAndWait();
        return controller;
    }

    /**
     * Открывает отдельное (немодальное) окно раздела. Если окно уже открыто — просто выводит его на передний план.
     */
    public static void openWindow(String fxml, String title, double width, double height) throws IOException {
        Stage existing = OPEN_WINDOWS.get(fxml);
        if (existing != null) {
            existing.setIconified(false);
            existing.toFront();
            existing.requestFocus();
            return;
        }
        FXMLLoader loader = loader(fxml);
        Parent root = loader.load();
        Object controller = loader.getController();
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(scene(root, width, height));
        stage.setMinWidth(Math.min(width, 700));
        stage.setMinHeight(Math.min(height, 420));
        if (controller instanceof Refreshable r) register(r);
        stage.setOnHidden(e -> {
            OPEN_WINDOWS.remove(fxml);
            if (controller instanceof Refreshable r) LISTENERS.remove(r);
        });
        OPEN_WINDOWS.put(fxml, stage);
        stage.show();
    }

    public static void register(Refreshable listener) { LISTENERS.add(listener); }

    /** Данные изменились — все открытые окна перечитывают их, чтобы показывать актуальное. */
    public static void refreshAll() { List.copyOf(LISTENERS).forEach(Refreshable::refresh); }

    public static Scene scene(Parent root) { return withStyle(new Scene(root)); }

    public static Scene scene(Parent root, double width, double height) { return withStyle(new Scene(root, width, height)); }

    public static FXMLLoader loader(String fxml) { return new FXMLLoader(WindowFactory.class.getResource(BASE + fxml)); }

    private static Scene withStyle(Scene scene) {
        scene.getStylesheets().add(WindowFactory.class.getResource(BASE + "style.css").toExternalForm());
        return scene;
    }
}
