package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import ru.mirea.callcenter.model.Appeal;
import ru.mirea.callcenter.model.AppealStatus;
import ru.mirea.callcenter.service.AppealDetails;
import ru.mirea.callcenter.service.AppealFilter;
import ru.mirea.callcenter.service.AppealFilter.SortField;
import ru.mirea.callcenter.service.AppealService;
import ru.mirea.callcenter.service.AppealStatistics;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.AlertUtil;
import ru.mirea.callcenter.ui.util.Labels;
import ru.mirea.callcenter.ui.util.Refreshable;
import ru.mirea.callcenter.ui.util.UiErrors;
import ru.mirea.callcenter.ui.util.WindowFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

public class MainController implements Refreshable {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppealService appealService = AppContext.appeals();
    /** Источник данных таблицы: при изменении списка TableView перерисовывается сам. */
    private final ObservableList<Appeal> appeals = FXCollections.observableArrayList();
    private boolean suppressReload;

    @FXML private TextField searchField;
    @FXML private ComboBox<AppealStatus> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<SortField> sortBox;
    @FXML private CheckBox activeOnlyCheck;
    @FXML private TableView<Appeal> appealTable;
    @FXML private TableColumn<Appeal, String> clientColumn;
    @FXML private TableColumn<Appeal, String> phoneColumn;
    @FXML private TableColumn<Appeal, String> topicColumn;
    @FXML private TableColumn<Appeal, String> categoryColumn;
    @FXML private TableColumn<Appeal, String> statusColumn;
    @FXML private TableColumn<Appeal, String> activeColumn;
    @FXML private TableColumn<Appeal, String> operatorColumn;
    @FXML private TableColumn<Appeal, LocalDateTime> createdColumn;
    @FXML private TableColumn<Appeal, LocalDateTime> closedColumn;
    @FXML private Label infoLabel;
    @FXML private Button editButton, viewButton, statusButton, deleteButton;

    @FXML
    private void initialize() {
        setupColumns();
        setupFilters();
        appealTable.setItems(appeals);

        // кнопки, которым нужна выбранная строка, неактивны, пока ничего не выбрано
        var noSelection = appealTable.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        viewButton.disableProperty().bind(noSelection);
        statusButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        appealTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && appealTable.getSelectionModel().getSelectedItem() != null) onView();
        });
        reload();
    }

    // ---------- настройка интерфейса ----------

    private void setupColumns() {
        clientColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getClientName()));
        phoneColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getClientPhone()));
        topicColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getTopic()));
        categoryColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Labels.category(c.getValue().getCategory())));
        statusColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(Labels.status(c.getValue().getStatus())));
        activeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActive() ? "Да" : "Нет"));
        operatorColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                c.getValue().getOperatorName() == null ? "не назначен" : c.getValue().getOperatorName()));
        createdColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getCreatedAt()));
        closedColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getClosedAt()));
        createdColumn.setCellFactory(col -> dateCell());
        closedColumn.setCellFactory(col -> dateCell());
    }

    private TableCell<Appeal, LocalDateTime> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "—" : DATE_TIME.format(item));
            }
        };
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(AppealStatus.values()));
        statusFilter.setConverter(converter(Labels::status));
        categoryFilter.setItems(FXCollections.observableArrayList(AppealService.categories()));
        categoryFilter.setConverter(converter(Labels::category));
        sortBox.setItems(FXCollections.observableArrayList(SortField.values()));
        sortBox.setConverter(converter(Labels::sort));
        sortBox.setValue(SortField.CREATED_NEWEST);

        // любое изменение поиска или фильтра сразу перезагружает таблицу
        searchField.textProperty().addListener((obs, old, now) -> reload());
        statusFilter.valueProperty().addListener((obs, old, now) -> reload());
        categoryFilter.valueProperty().addListener((obs, old, now) -> reload());
        fromDate.valueProperty().addListener((obs, old, now) -> reload());
        toDate.valueProperty().addListener((obs, old, now) -> reload());
        sortBox.valueProperty().addListener((obs, old, now) -> reload());
        activeOnlyCheck.selectedProperty().addListener((obs, old, now) -> reload());
    }

    private <T> StringConverter<T> converter(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : toText.apply(value); }
            @Override public T fromString(String text) { return null; }
        };
    }

    // ---------- загрузка данных ----------

    private AppealFilter currentFilter() {
        return new AppealFilter(searchField.getText(), statusFilter.getValue(), categoryFilter.getValue(),
                fromDate.getValue(), toDate.getValue(), sortBox.getValue(), activeOnlyCheck.isSelected());
    }

    /** Вызывается после изменений в любом окне (например, переименовали клиента). */
    @Override
    public void refresh() { reload(); }

    private void reload() {
        if (suppressReload) return;
        Appeal selected = appealTable.getSelectionModel().getSelectedItem();
        try {
            List<Appeal> found = appealService.find(currentFilter());
            appeals.setAll(found);
            // после перезагрузки возвращаем выделение на то же обращение
            if (selected != null) found.stream().filter(a -> a.getId().equals(selected.getId())).findFirst()
                    .ifPresent(a -> appealTable.getSelectionModel().select(a));
            showInfo("Показано обращений: " + found.size(), false);
        } catch (Exception e) {
            showInfo(UiErrors.message(e), true);
        }
    }

    private void showInfo(String text, boolean error) {
        infoLabel.setText(text);
        infoLabel.getStyleClass().remove("error-label");
        if (error) infoLabel.getStyleClass().add("error-label");
    }

    // ---------- обработчики кнопок ----------

    @FXML
    private void onRefresh() { reload(); }

    @FXML
    private void onAdd() { openForm(null); }

    @FXML
    private void onEdit() {
        Appeal selected = requireSelection();
        if (selected != null) openForm(selected);
    }

    @FXML
    private void onView() {
        Appeal selected = requireSelection();
        if (selected == null) return;
        AlertUtil.run(() -> {
            AppealDetails details = appealService.getDetails(selected.getId());
            WindowFactory.<DetailsController>openModal(owner(), "details-view.fxml", "Сведения об обращении",
                    c -> c.setDetails(details));
        });
    }

    @FXML
    private void onStats() {
        AppealStatistics stats = appealService.statistics(new ArrayList<>(appeals));
        AlertUtil.run(() -> WindowFactory.<StatsController>openModal(owner(), "stats-view.fxml", "Статистика",
                c -> c.setStatistics(stats)));
    }

    @FXML
    private void onExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт обращений");
        chooser.setInitialFileName("appeals");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"),
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(owner());
        if (file == null) return;
        String name = file.getName().toLowerCase();
        Path path = name.endsWith(".xlsx") || name.endsWith(".csv") ? file.toPath()
                : Path.of(file.getPath() + chooser.getSelectedExtensionFilter().getExtensions().get(0).substring(1));
        List<Appeal> snapshot = new ArrayList<>(appeals);
        if (AlertUtil.run(() -> AppContext.export().export(snapshot, path)))
            AlertUtil.info("Экспорт", "Экспортировано записей: " + snapshot.size() + "\nФайл: " + path);
    }

    private void openForm(Appeal appeal) {
        AlertUtil.run(() -> {
            Appeal fresh = appeal == null ? null : appealService.getById(appeal.getId());
            AppealFormController form = WindowFactory.<AppealFormController>openModal(owner(), "appeal-form.fxml",
                    appeal == null ? "Новое обращение" : "Редактирование обращения", c -> c.setAppeal(fresh));
            if (form.isSaved()) WindowFactory.refreshAll();
        });
    }

    private Window owner() { return appealTable.getScene().getWindow(); }

    @FXML
    private void onResetFilters() {
        suppressReload = true;           // чтобы не перезагружать таблицу после каждого поля
        searchField.clear();
        statusFilter.setValue(null);
        categoryFilter.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
        sortBox.setValue(SortField.CREATED_NEWEST);
        activeOnlyCheck.setSelected(false);
        suppressReload = false;
        reload();
    }

    @FXML
    private void onChangeStatus() {
        Appeal selected = requireSelection();
        if (selected == null) return;
        List<AppealStatus> next = appealService.nextStatuses(selected.getStatus());
        if (next.isEmpty()) {
            AlertUtil.info("Смена статуса", "Из статуса «" + Labels.status(selected.getStatus()) + "» переходов нет.");
            return;
        }
        Map<String, AppealStatus> options = new LinkedHashMap<>();
        next.forEach(s -> options.put(Labels.status(s), s));
        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.keySet().iterator().next(), options.keySet());
        dialog.setTitle("Смена статуса");
        dialog.setHeaderText("Текущий статус: " + Labels.status(selected.getStatus()));
        dialog.setContentText("Новый статус:");
        dialog.showAndWait().ifPresent(choice -> {
            if (AlertUtil.run(() -> appealService.changeStatus(selected.getId(), options.get(choice)))) WindowFactory.refreshAll();
        });
    }

    @FXML
    private void onDelete() {
        Appeal selected = requireSelection();
        if (selected == null) return;
        boolean confirmed = AlertUtil.confirm("Удаление обращения",
                "Удалить обращение клиента «" + selected.getClientName() + "»?",
                "Тема: " + selected.getTopic() + "\nДействие нельзя отменить.");
        if (confirmed && AlertUtil.run(() -> appealService.delete(selected.getId()))) WindowFactory.refreshAll();
    }

    private Appeal requireSelection() {
        Appeal selected = appealTable.getSelectionModel().getSelectedItem();
        if (selected == null) AlertUtil.info("Не выбрана запись", "Сначала выберите обращение в таблице.");
        return selected;
    }
}
