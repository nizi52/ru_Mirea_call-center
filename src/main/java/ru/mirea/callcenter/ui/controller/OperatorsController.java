package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.util.StringConverter;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.service.OperatorService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;

public class OperatorsController extends CrudTabController<Operator> {
    private final OperatorService operatorService = AppContext.operators();
    private boolean suppressReload;

    @FXML private ComboBox<CallCenter> callCenterFilter;
    @FXML private CheckBox activeOnly;
    @FXML private TableColumn<Operator, Integer> idColumn;
    @FXML private TableColumn<Operator, String> nameColumn;
    @FXML private TableColumn<Operator, String> loginColumn;
    @FXML private TableColumn<Operator, String> callCenterColumn;
    @FXML private TableColumn<Operator, String> activeColumn;

    @Override
    protected void setupColumns() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        loginColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getLogin()));
        callCenterColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCallCenterName()));
        activeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActive() ? "Активен" : "Неактивен"));
    }

    @Override
    protected void setupFilters() {
        callCenterFilter.setConverter(new StringConverter<>() {
            @Override public String toString(CallCenter d) { return d == null ? "" : d.getName(); }
            @Override public CallCenter fromString(String s) { return null; }
        });
        callCenterFilter.valueProperty().addListener((obs, old, now) -> { if (!suppressReload) reload(); });
        activeOnly.selectedProperty().addListener((obs, old, now) -> reload());
    }

    @Override
    public void refresh() {
        // список колл-центров мог измениться на соседней вкладке
        CallCenter current = callCenterFilter.getValue();
        suppressReload = true;
        try {
            List<CallCenter> all = AppContext.callCenters().getAll();
            callCenterFilter.setItems(FXCollections.observableArrayList(all));
            callCenterFilter.setValue(current == null ? null : all.stream()
                    .filter(d -> d.getId().equals(current.getId())).findFirst().orElse(null));
        } catch (Exception ignored) {
            // ошибку подключения покажет reload() ниже
        } finally {
            suppressReload = false;
        }
        reload();
    }

    @Override
    protected List<Operator> load(String query) throws Exception {
        CallCenter callCenter = callCenterFilter.getValue();
        return operatorService.search(query, callCenter == null ? null : callCenter.getId(), activeOnly.isSelected());
    }

    @Override
    protected void resetExtraFilters() {
        suppressReload = true;
        callCenterFilter.setValue(null);
        activeOnly.setSelected(false);
        suppressReload = false;
    }

    @Override
    protected boolean openForm(Operator editing) throws Exception {
        OperatorFormController form = WindowFactory.openModal(window(), "operator-form.fxml",
                editing == null ? "Новый оператор" : "Редактирование оператора", f -> f.setOperator(editing));
        return form.isSaved();
    }

    @Override
    protected void delete(Operator item) throws Exception { operatorService.delete(item.getId()); }

    @Override
    protected String describe(Operator item) { return "оператора «" + item.getFullName() + "»"; }

    @Override
    protected String countText(int count) { return "Операторов: " + count; }
}
