package ru.mirea.callcenter.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.function.Consumer;

public final class WindowFactory {
    private static final String BASE = "/ru/mirea/callcenter/ui/";

    private WindowFactory() { }

    /** Загружает FXML, передаёт контроллер в init, показывает окно и ждёт его закрытия. */
    public static <T> T openModal(Window owner, String fxml, String title, Consumer<T> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(WindowFactory.class.getResource(BASE + fxml));
        Parent root = loader.load();
        T controller = loader.getController();
        if (init != null) init.accept(controller);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(WindowFactory.class.getResource(BASE + "style.css").toExternalForm());
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.showAndWait();
        return controller;
    }
}
