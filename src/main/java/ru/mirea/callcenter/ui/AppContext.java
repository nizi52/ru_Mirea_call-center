package ru.mirea.callcenter.ui;

import ru.mirea.callcenter.repository.*;
import ru.mirea.callcenter.service.*;

public final class AppContext {
    private static final CallCenterService CALL_CENTERS = new CallCenterService(new CallCenterRepository());
    private static final ClientService CLIENTS = new ClientService(new ClientRepository());
    private static final OperatorService OPERATORS = new OperatorService(new OperatorRepository(), CALL_CENTERS);
    private static final AppealService APPEALS = new AppealService(new AppealRepository(), OPERATORS, CLIENTS);

    private static final ExportService EXPORT = new ExportService();

    private AppContext() { }

    public static ExportService export() { return EXPORT; }

    public static CallCenterService callCenters() { return CALL_CENTERS; }
    public static ClientService clients() { return CLIENTS; }
    public static OperatorService operators() { return OPERATORS; }
    public static AppealService appeals() { return APPEALS; }
}
