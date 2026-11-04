package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

import java.util.List;

/**
 * Главное окно с вкладками. Контроллеры вкладок подставляет FXMLLoader из fx:include
 * (поле = fx:id + "Controller"). При переключении вкладка перечитывает данные,
 * чтобы видеть изменения, сделанные на других вкладках.
 */
public class ShellController {
    @FXML private TabPane tabs;
    @FXML private MainController appealsController;
    @FXML private ClientsController clientsController;
    @FXML private OperatorsController operatorsController;
    @FXML private CallCentersController callCentersController;

    @FXML
    private void initialize() {
        List<Refreshable> byTab = List.of(appealsController, clientsController, operatorsController, callCentersController);
        tabs.getSelectionModel().selectedIndexProperty().addListener((obs, old, index) -> {
            int i = index.intValue();
            if (i >= 0 && i < byTab.size()) byTab.get(i).refresh();
        });
    }
}
