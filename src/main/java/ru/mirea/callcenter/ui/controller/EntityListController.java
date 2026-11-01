package ru.mirea.callcenter.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import ru.mirea.callcenter.ui.util.AlertUtil;
import ru.mirea.callcenter.ui.util.Refreshable;
import ru.mirea.callcenter.ui.util.UiErrors;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;
import java.util.Objects;

/**
 * Общая логика окна-списка (клиенты, операторы, колл-центры): таблица, поиск, добавление, изменение, удаление.
 * Подкласс описывает только то, что отличается: столбцы, загрузку, поиск и форму.
 */
public abstract class EntityListController<T> implements Refreshable {
    /** Все записи из БД. Таблица показывает их через фильтр (поиск) и сортировку по клику на заголовок. */
    protected final ObservableList<T> items = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(items);

    @FXML protected TextField searchField;
    @FXML protected TableView<T> table;
    @FXML protected Label infoLabel;
    @FXML protected Button editButton, deleteButton;

    // ---------- что задаёт подкласс ----------

    protected abstract void setupColumns();
    protected abstract List<T> load() throws Exception;
    protected abstract boolean matches(T item, String query);
    protected abstract Integer idOf(T item);
    /** Например, «Показано клиентов». */
    protected abstract String countCaption();
    /** Открывает форму (null — новая запись), возвращает true, если запись сохранена. */
    protected abstract boolean openForm(T item) throws Exception;
    protected abstract String deleteQuestion(T item);
    protected abstract void delete(T item) throws Exception;

    /** Дополнительные фильтры окна (по умолчанию нет). */
    protected boolean extraFilter(T item) { return true; }
    protected void setupExtraFilters() { }

    // ---------- общая логика ----------

    @FXML
    protected void initialize() {
        setupColumns();
        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selected() != null) onEdit();
        });
        searchField.textProperty().addListener((obs, old, now) -> applyFilter());
        setupExtraFilters();
        refresh();
    }

    @Override
    public void refresh() {
        T current = selected();
        Integer selectedId = current == null ? null : idOf(current);
        try {
            items.setAll(load());
            applyFilter();
            // после перезагрузки возвращаем выделение на ту же запись
            if (selectedId != null) table.getItems().stream().filter(x -> Objects.equals(idOf(x), selectedId))
                    .findFirst().ifPresent(x -> table.getSelectionModel().select(x));
        } catch (Exception e) {
            showInfo(UiErrors.message(e), true);
        }
    }

    protected void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filtered.setPredicate(x -> (q.isEmpty() || matches(x, q)) && extraFilter(x));
        showInfo(countCaption() + ": " + filtered.size() + " из " + items.size(), false);
    }

    protected static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    @FXML
    protected void onAdd() {
        AlertUtil.run(() -> { if (openForm(null)) WindowFactory.refreshAll(); });
    }

    @FXML
    protected void onEdit() {
        T item = selected();
        if (item == null) return;
        AlertUtil.run(() -> { if (openForm(item)) WindowFactory.refreshAll(); });
    }

    @FXML
    protected void onDelete() {
        T item = selected();
        if (item == null) return;
        if (!AlertUtil.confirm("Удаление", deleteQuestion(item), "Действие нельзя отменить.")) return;
        if (AlertUtil.run(() -> delete(item))) WindowFactory.refreshAll();
    }

    @FXML
    protected void onRefresh() { refresh(); }

    protected T selected() { return table.getSelectionModel().getSelectedItem(); }

    protected Window owner() { return table.getScene().getWindow(); }

    private void showInfo(String text, boolean error) {
        infoLabel.setText(text);
        infoLabel.getStyleClass().remove("error-label");
        if (error) infoLabel.getStyleClass().add("error-label");
    }
}
