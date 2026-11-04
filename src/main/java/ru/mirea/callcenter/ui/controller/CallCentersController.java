package ru.mirea.callcenter.ui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import ru.mirea.callcenter.model.CallCenter;
import ru.mirea.callcenter.service.CallCenterService;
import ru.mirea.callcenter.ui.AppContext;
import ru.mirea.callcenter.ui.util.WindowFactory;

import java.util.List;

public class CallCentersController extends CrudTabController<CallCenter> {
    private final CallCenterService callCenterService = AppContext.callCenters();

    @FXML private TableColumn<CallCenter, Integer> idColumn;
    @FXML private TableColumn<CallCenter, String> nameColumn;
    @FXML private TableColumn<CallCenter, String> addressColumn;
    @FXML private TableColumn<CallCenter, String> activeColumn;

    @Override
    protected void setupColumns() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        nameColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        addressColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAddress()));
        activeColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().isActive() ? "Активен" : "Неактивен"));
    }

    @Override
    protected List<CallCenter> load(String query) throws Exception { return callCenterService.search(query); }

    @Override
    protected boolean openForm(CallCenter editing) throws Exception {
        CallCenterFormController form = WindowFactory.openModal(window(), "call-center-form.fxml",
                editing == null ? "Новый колл-центр" : "Редактирование колл-центра", f -> f.setCallCenter(editing));
        return form.isSaved();
    }

    @Override
    protected void delete(CallCenter item) throws Exception { callCenterService.delete(item.getId()); }

    @Override
    protected String describe(CallCenter item) { return "колл-центр «" + item.getName() + "»"; }

    @Override
    protected String countText(int count) { return "Колл-центров: " + count; }
}
