package ru.mirea.callcenter.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1150, 680);
        scene.getStylesheets().add(App.class.getResource("style.css").toExternalForm());
        stage.setTitle("Колл-центр — обращения клиентов");
        stage.setMinWidth(900);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
