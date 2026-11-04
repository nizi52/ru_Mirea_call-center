package ru.mirea.callcenter.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.mirea.callcenter.ui.util.AlertUtil;
import ru.mirea.callcenter.ui.util.UiErrors;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Общая логика вкладок-справочников (клиенты, операторы, колл-центры):
 * таблица на ObservableList, поиск, кнопки «Добавить / Изменить / Удалить / Обновить».
 * Наследник описывает только столбцы, загрузку, форму и удаление своей сущности.
 */
public abstract class CrudTabController<T> implements Initializable, Refreshable {
    protected final ObservableList<T> items = FXCollections.observableArrayList();

    @FXML protected TableView<T> table;
    @FXML protected TextField searchField;
    @FXML protected Label infoLabel;
    @FXML protected Button editButton;
    @FXML protected Button deleteButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        setupFilters();
        table.setItems(items);
        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selected() != null) onEdit();
        });
        searchField.textProperty().addListener((obs, old, now) -> reload());
        // данные загружает главное окно при открытии вкладки (refresh), чтобы не ходить в БД заранее
    }

    protected abstract void setupColumns();

    /** Дополнительные фильтры вкладки; по умолчанию только поле поиска. */
    protected void setupFilters() { }

    protected abstract List<T> load(String query) throws Exception;

    /** Открывает форму; editing == null — создание. Возвращает true, если данные сохранили. */
    protected abstract boolean openForm(T editing) throws Exception;

    protected abstract void delete(T item) throws Exception;

    /** Как назвать запись в диалоге подтверждения удаления. */
    protected abstract String describe(T item);

    /** Подпись «Найдено: N …». */
    protected abstract String countText(int count);

    @Override
    public void refresh() { reload(); }

    protected void reload() {
        try {
            items.setAll(load(searchField.getText()));
            infoLabel.setText(countText(items.size()));
        } catch (Exception e) {
            items.clear();
            infoLabel.setText("");
            AlertUtil.error(UiErrors.message(e));
        }
    }

    protected T selected() { return table.getSelectionModel().getSelectedItem(); }

    protected Window window() { return table.getScene().getWindow(); }

    @FXML
    protected void onAdd() {
        AlertUtil.run(() -> { if (openForm(null)) reload(); });
    }

    @FXML
    protected void onEdit() {
        T item = selected();
        if (item == null) return;
        AlertUtil.run(() -> { if (openForm(item)) reload(); });
    }

    @FXML
    protected void onDelete() {
        T item = selected();
        if (item == null) return;
        boolean confirmed = AlertUtil.confirm("Удаление", "Удалить " + describe(item) + "?",
                "Действие нельзя отменить.");
        if (confirmed && AlertUtil.run(() -> delete(item))) reload();
    }

    @FXML
    protected void onRefresh() { reload(); }

    @FXML
    protected void onResetFilters() {
        searchField.clear();
        resetExtraFilters();
        reload();
    }

    /** Сброс дополнительных фильтров наследника. */
    protected void resetExtraFilters() { }
}
