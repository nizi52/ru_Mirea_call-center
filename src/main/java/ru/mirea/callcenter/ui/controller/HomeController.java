package ru.mirea.callcenter.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.AppealStatus;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.AlertUtil;
import ru.mirea.callcenter.ui.util.Refreshable;
import ru.mirea.callcenter.ui.util.UiErrors;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;

/** Главное меню: из него каждая сущность открывается в своём отдельном окне. */
public class HomeController implements Refreshable {
    @FXML private Label appealsCount, clientsCount, operatorsCount, callCentersCount, statusLabel;

    @FXML
    private void initialize() { refresh(); }

    @Override
    public void refresh() {
        try {
            List<Appeal> appeals = AppContext.appeals().getAll();
            long open = appeals.stream().filter(a -> a.getStatus() != AppealStatus.CLOSED).count();
            List<Operator> operators = AppContext.operators().getAll();
            List<CallCenter> centers = AppContext.callCenters().getAll();
            appealsCount.setText(appeals.size() + " всего, не закрыто " + open);
            clientsCount.setText(AppContext.clients().getAll().size() + " в базе");
            operatorsCount.setText(operators.stream().filter(Operator::isActive).count() + " активных из " + operators.size());
            callCentersCount.setText(centers.stream().filter(CallCenter::isActive).count() + " работают из " + centers.size());
            statusLabel.setText("База данных подключена");
            statusLabel.getStyleClass().remove("error-label");
        } catch (Exception e) {
            statusLabel.setText(UiErrors.message(e));
            if (!statusLabel.getStyleClass().contains("error-label")) statusLabel.getStyleClass().add("error-label");
        }
    }

    @FXML
    private void onAppeals() { open("main-view.fxml", "Обращения клиентов", 1150, 680); }

    @FXML
    private void onClients() { open("clients-view.fxml", "Клиенты", 860, 560); }

    @FXML
    private void onOperators() { open("operators-view.fxml", "Операторы", 960, 560); }

    @FXML
    private void onCallCenters() { open("call-centers-view.fxml", "Колл-центры", 860, 520); }

    private void open(String fxml, String title, double width, double height) {
        AlertUtil.run(() -> WindowFactory.openWindow(fxml, title, width, height));
    }
}
