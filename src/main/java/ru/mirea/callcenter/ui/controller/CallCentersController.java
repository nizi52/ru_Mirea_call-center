package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.model.Operator;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CallCentersController extends EntityListController<CallCenter> {
    @FXML private TableColumn<CallCenter, String> nameColumn, addressColumn, activeColumn;
    @FXML private TableColumn<CallCenter, Number> operatorsColumn;

    /** Сколько операторов в каждом колл-центре (call_center_id → количество). */
    private Map<Integer, Long> operatorCounts = Map.of();

    @Override
    protected void setupColumns() {
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        addressColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAddress()));
        activeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActive() ? "Работает" : "Не работает"));
        operatorsColumn.setCellValueFactory(c -> new ReadOnlyLongWrapper(operatorCounts.getOrDefault(c.getValue().getId(), 0L)));
    }

    @Override
    protected List<CallCenter> load() throws Exception {
        operatorCounts = AppContext.operators().getAll().stream()
                .collect(Collectors.groupingBy(Operator::getCallCenterId, Collectors.counting()));
        return AppContext.callCenters().getAll();
    }

    @Override
    protected boolean matches(CallCenter c, String q) { return contains(c.getName(), q) || contains(c.getAddress(), q); }

    @Override protected Integer idOf(CallCenter c) { return c.getId(); }
    @Override protected String countCaption() { return "Показано колл-центров"; }

    @Override
    protected boolean openForm(CallCenter callCenter) throws Exception {
        CallCenter fresh = callCenter == null ? null : AppContext.callCenters().getById(callCenter.getId());
        return WindowFactory.<CallCenterFormController>openModal(owner(), "call-center-form.fxml",
                callCenter == null ? "Новый колл-центр" : "Редактирование колл-центра", c -> c.setEntity(fresh)).isSaved();
    }

    @Override
    protected String deleteQuestion(CallCenter c) { return "Удалить колл-центр «" + c.getName() + "»?"; }

    @Override
    protected void delete(CallCenter c) throws Exception { AppContext.callCenters().delete(c.getId()); }
}
