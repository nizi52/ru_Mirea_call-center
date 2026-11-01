package ru.mirea.callcenter.ui;

import ru.mirea.callcenter.repository.*;
import ru.mirea.callcenter.service.*;

public final class AppContext {
    private static final DepartmentService DEPARTMENTS = new DepartmentService(new DepartmentRepository());
    private static final ClientService CLIENTS = new ClientService(new ClientRepository());
    private static final OperatorService OPERATORS = new OperatorService(new OperatorRepository(), DEPARTMENTS);
    private static final AppealService APPEALS = new AppealService(new AppealRepository(), OPERATORS, CLIENTS);

    private static final ExportService EXPORT = new ExportService();

    private AppContext() { }

    public static ExportService export() { return EXPORT; }

    public static ClientService clients() { return CLIENTS; }
    public static OperatorService operators() { return OPERATORS; }
    public static AppealService appeals() { return APPEALS; }
}
