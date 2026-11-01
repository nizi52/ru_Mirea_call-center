package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import ru.mirea.callcenter.service.AppealStatistics;
import ru.mirea.callcenter.ui.util.Labels;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatsController {
    @FXML private Label totalValue, openValue, avgValue, escalatedValue;
    @FXML private TableView<Map.Entry<String, Long>> statusTable, categoryTable, operatorTable;
    @FXML private TableColumn<Map.Entry<String, Long>, String> statusNameColumn, categoryNameColumn, operatorNameColumn;
    @FXML private TableColumn<Map.Entry<String, Long>, Number> statusCountColumn, categoryCountColumn, operatorCountColumn;

    public void setStatistics(AppealStatistics s) {
        totalValue.setText(String.valueOf(s.total()));
        openValue.setText(String.valueOf(s.open()));
        avgValue.setText(formatMinutes(s.avgCloseMinutes()));
        escalatedValue.setText("%.1f %%".formatted(s.escalatedPercent()));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        s.byStatus().forEach((status, count) -> byStatus.put(Labels.status(status), count));
        Map<String, Long> byCategory = new LinkedHashMap<>();
        s.byCategory().forEach((code, count) -> byCategory.put(Labels.category(code), count));

        fill(statusTable, statusNameColumn, statusCountColumn, byStatus);
        fill(categoryTable, categoryNameColumn, categoryCountColumn, byCategory);
        fill(operatorTable, operatorNameColumn, operatorCountColumn, s.byOperator());
    }

    private void fill(TableView<Map.Entry<String, Long>> table, TableColumn<Map.Entry<String, Long>, String> nameColumn,
                      TableColumn<Map.Entry<String, Long>, Number> countColumn, Map<String, Long> data) {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getKey()));
        countColumn.setCellValueFactory(c -> new ReadOnlyLongWrapper(c.getValue().getValue()));
        table.setItems(FXCollections.observableArrayList(data.entrySet()));
    }

    private String formatMinutes(double minutes) {
        long m = Math.round(minutes);
        if (m == 0) return "—";
        return m < 60 ? m + " мин" : (m / 60) + " ч " + (m % 60) + " мин";
    }

    @FXML
    private void onClose() { ((Stage) totalValue.getScene().getWindow()).close(); }
}
