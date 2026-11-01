package ru.mirea.callcenter.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import ru.mirea.callcenter.ui.util.Refreshable;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // стартовое окно — главное меню; обращения, клиенты, операторы и колл-центры открываются из него в отдельных окнах
        FXMLLoader loader = WindowFactory.loader("home-view.fxml");
        stage.setScene(WindowFactory.scene(loader.load(), 640, 460));
        if (loader.getController() instanceof Refreshable home) WindowFactory.register(home);
        stage.setTitle("Колл-центр");
        stage.setMinWidth(560);
        stage.setMinHeight(420);
        // закрыли главное меню — закрываем и все окна разделов
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
