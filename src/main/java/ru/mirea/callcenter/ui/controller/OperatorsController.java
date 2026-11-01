package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.service.AppealService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.Converters;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OperatorsController extends EntityListController<Operator> {
    @FXML private TableColumn<Operator, String> nameColumn, loginColumn, callCenterColumn, activeColumn, loadColumn;
    @FXML private ComboBox<CallCenter> callCenterFilter;
    @FXML private CheckBox activeOnlyCheck;

    /** Активные обращения по операторам (operator_id → количество). */
    private Map<Integer, Long> activeCounts = Map.of();

    @Override
    protected void setupColumns() {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getFullName()));
        loginColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getLogin()));
        callCenterColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCallCenterName()));
        activeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActive() ? "Активен" : "Неактивен"));
        loadColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(
                activeCounts.getOrDefault(c.getValue().getId(), 0L) + " из " + AppealService.maxActivePerOperator()));
    }

    @Override
    protected void setupExtraFilters() {
        callCenterFilter.setConverter(Converters.of(CallCenter::getName));
        callCenterFilter.valueProperty().addListener((obs, old, now) -> applyFilter());
        activeOnlyCheck.selectedProperty().addListener((obs, old, now) -> applyFilter());
    }

    @Override
    protected boolean extraFilter(Operator o) {
        CallCenter cc = callCenterFilter.getValue();
        return (cc == null || Objects.equals(cc.getId(), o.getCallCenterId()))
                && (!activeOnlyCheck.isSelected() || o.isActive());
    }

    @Override
    protected List<Operator> load() throws Exception {
        activeCounts = AppContext.appeals().activeCountByOperator();
        // список колл-центров для фильтра обновляем, сохраняя выбранный
        Integer chosen = callCenterFilter.getValue() == null ? null : callCenterFilter.getValue().getId();
        List<CallCenter> centers = AppContext.callCenters().getAll();
        callCenterFilter.setItems(FXCollections.observableArrayList(centers));
        centers.stream().filter(c -> c.getId().equals(chosen)).findFirst().ifPresent(callCenterFilter::setValue);
        return AppContext.operators().getAll();
    }

    @Override
    protected boolean matches(Operator o, String q) {
        return contains(o.getFullName(), q) || contains(o.getLogin(), q) || contains(o.getCallCenterName(), q);
    }

    @Override protected Integer idOf(Operator o) { return o.getId(); }
    @Override protected String countCaption() { return "Показано операторов"; }

    @Override
    protected boolean openForm(Operator operator) throws Exception {
        Operator fresh = operator == null ? null : AppContext.operators().getById(operator.getId());
        return WindowFactory.<OperatorFormController>openModal(owner(), "operator-form.fxml",
                operator == null ? "Новый оператор" : "Редактирование оператора", c -> c.setEntity(fresh)).isSaved();
    }

    @Override
    protected String deleteQuestion(Operator o) {
        return "Удалить оператора «" + o.getFullName() + "»?\nЕго новые обращения станут неназначенными.";
    }

    @Override
    protected void delete(Operator o) throws Exception { AppContext.operators().delete(o.getId()); }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        callCenterFilter.setValue(null);
        activeOnlyCheck.setSelected(false);
    }
}
