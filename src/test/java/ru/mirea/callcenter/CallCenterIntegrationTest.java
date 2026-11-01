package ru.mirea.callcenter;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.model.*;
import ru.mirea.callcenter.repository.*;
import ru.mirea.callcenter.service.*;
import ru.mirea.callcenter.util.CsvExporter;
import ru.mirea.callcenter.util.ExcelExporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CallCenterIntegrationTest {
    private static AppealFilter filter(String text, AppealStatus status, LocalDate from, LocalDate to,
                                       AppealFilter.SortField sort) {
        return new AppealFilter(text, status, null, from, to, sort);
    }

    @Test
    void fourEntitiesBusinessRulesSearchStatisticsAndExport() throws Exception {
        assumeTrue(System.getenv("CALLCENTER_DB_URL") != null, "Set CALLCENTER_DB_URL to an isolated test database");

        CallCenterService callCenters = new CallCenterService(new CallCenterRepository());
        ClientService clients = new ClientService(new ClientRepository());
        OperatorService operators = new OperatorService(new OperatorRepository(), callCenters);
        AppealService appeals = new AppealService(new AppealRepository(), operators, clients);
        String suffix = Long.toString(System.nanoTime());

        CallCenter callCenter = callCenters.create(new CallCenter("Тест " + suffix, "Тестовый адрес", true));
        Client client = clients.create(new Client("Тестовый клиент", "+7" + suffix, "test@example.org"));
        Operator operator = operators.create(new Operator(null, "Тестовый оператор", "test" + suffix, callCenter.getId(), null, true));
        Appeal appeal = null;
        try {
            assertEquals(callCenter.getId(), operators.getById(operator.getId()).getCallCenterId());
            assertEquals(client.getPhone(), clients.getById(client.getId()).getPhone());
            client.setFullName("Обновлённый клиент");
            clients.update(client);
            callCenter.setAddress("Обновлённый адрес");
            callCenters.update(callCenter);
            operator.setFullName("Обновлённый оператор");
            operators.update(operator);
            assertEquals("Обновлённый оператор", operators.getById(operator.getId()).getFullName());
            assertThrows(BusinessException.class, () -> appeals.create(new Appeal(null, operator.getId(), client.getId(),
                    null, null, " ", "TECH", AppealStatus.NEW, null, null)));
            assertThrows(ru.mirea.callcenter.exception.EntityNotFoundException.class,
                    () -> appeals.create(new Appeal(null, 999999, client.getId(), null, null,
                            "Несуществующий оператор", "TECH", AppealStatus.NEW, null, null)));

            appeal = appeals.create(new Appeal(null, operator.getId(), client.getId(), null, null,
                    "Проверка " + suffix, "TECH", AppealStatus.NEW, null, null));
            int appealId = appeal.getId();
            assertEquals(client.getId(), appeals.getById(appeal.getId()).getClientId());
            assertEquals("Обновлённый клиент", appeals.getById(appeal.getId()).getClientName());
            appeal.setTopic("Обновлённая тема " + suffix);
            appeals.updateDetails(appeal);
            assertEquals(appeal.getTopic(), appeals.getById(appeal.getId()).getTopic());
            assertThrows(BusinessException.class, () -> appeals.changeStatus(appealId, AppealStatus.CLOSED));
            assertFalse(appeals.find(filter("Обновлённый клиент", null, null, null, AppealFilter.SortField.ID)).isEmpty());
            assertFalse(appeals.find(filter(suffix, null, null, null, AppealFilter.SortField.ID)).isEmpty());
            assertTrue(appeals.find(filter(null, AppealStatus.NEW, null, null, AppealFilter.SortField.ID))
                    .stream().anyMatch(a -> a.getId().equals(appealId)));
            assertTrue(appeals.find(filter(null, null, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                            AppealFilter.SortField.CREATED_NEWEST))
                    .stream().anyMatch(a -> a.getId().equals(appealId)));
            assertFalse(appeals.find(filter(null, null, null, null, AppealFilter.SortField.STATUS)).isEmpty());
            assertThrows(BusinessException.class, () -> appeals.find(
                    filter(null, null, LocalDate.now(), LocalDate.now().minusDays(1), AppealFilter.SortField.ID)));

            appeals.changeStatus(appeal.getId(), AppealStatus.IN_PROGRESS);
            appeals.changeStatus(appeal.getId(), AppealStatus.ESCALATED);
            appeals.changeStatus(appeal.getId(), AppealStatus.RESOLVED);
            appeals.changeStatus(appeal.getId(), AppealStatus.CLOSED);
            assertNotNull(appeals.getById(appeal.getId()).getClosedAt());
            assertTrue(appeals.statistics(appeals.getAll()).total() > 0);
            assertNotNull(appeals.getDetails(appealId).operator());

            Path directory = Files.createTempDirectory("callcenter-export-");
            Path xlsx = directory.resolve("appeals.xlsx");
            Path csv = directory.resolve("appeals.csv");
            new ExcelExporter().export(appeals.getAll(), xlsx);
            new CsvExporter().export(appeals.getAll(), csv);
            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(xlsx))) {
                assertEquals(appeals.getAll().size() + 1, workbook.getSheetAt(0).getPhysicalNumberOfRows());
            }
            assertTrue(Files.readString(csv).contains(suffix));

            List<Appeal> active = new ArrayList<>();
            try {
                for (int i = 0; i < 5; i++) {
                    active.add(appeals.create(new Appeal(null, operator.getId(), client.getId(), null, null,
                            "Нагрузка " + i, "TECH", AppealStatus.NEW, null, null)));
                }
                assertThrows(BusinessException.class, () -> appeals.create(new Appeal(null, operator.getId(), client.getId(),
                        null, null, "Шестое обращение", "TECH", AppealStatus.NEW, null, null)));
            } finally {
                for (Appeal item : active) appeals.delete(item.getId());
            }
        } finally {
            if (appeal != null) appeals.delete(appeal.getId());
            operators.delete(operator.getId());
            clients.delete(client.getId());
            callCenters.delete(callCenter.getId());
        }
    }

    @Test
    void directoryWindowsRules() throws Exception {
        assumeTrue(System.getenv("CALLCENTER_DB_URL") != null, "Set CALLCENTER_DB_URL to an isolated test database");

        CallCenterService callCenters = new CallCenterService(new CallCenterRepository());
        ClientService clients = new ClientService(new ClientRepository());
        OperatorService operators = new OperatorService(new OperatorRepository(), callCenters);
        AppealService appeals = new AppealService(new AppealRepository(), operators, clients);
        String suffix = Long.toString(System.nanoTime()).substring(4);

        // неверные поля: исключение указывает, какое поле подсветить в форме
        assertEquals("phone", assertThrows(BusinessException.class,
                () -> clients.create(new Client("Клиент", "12-34", null))).getField());
        assertEquals("email", assertThrows(BusinessException.class,
                () -> clients.create(new Client("Клиент", "+7" + suffix, "не-почта"))).getField());
        assertEquals("login", assertThrows(BusinessException.class,
                () -> operators.create(new Operator(null, "Оператор", "логин кириллицей", 1, null, true))).getField());

        CallCenter callCenter = callCenters.create(new CallCenter("  Тест окна " + suffix + "  ", "Адрес", true));
        Client client = clients.create(new Client(" Клиент окна ", "+7 (" + suffix.substring(0, 3) + ") " + suffix.substring(3), ""));
        Operator operator = operators.create(new Operator(null, "Оператор окна", "win" + suffix, callCenter.getId(), null, true));
        Appeal appeal = null;
        try {
            assertEquals("Тест окна " + suffix, callCenter.getName());           // пробелы обрезаны
            assertEquals("+7" + suffix, client.getPhone());                       // скобки и пробелы убраны
            assertNull(client.getEmail());                                        // пустой email → null

            // дубликаты дают понятную ошибку по полю, а не общую ошибку БД
            assertEquals("phone", assertThrows(BusinessException.class,
                    () -> clients.create(new Client("Другой", "+7" + suffix, null))).getField());
            assertEquals("login", assertThrows(BusinessException.class,
                    () -> operators.create(new Operator(null, "Другой", "win" + suffix, callCenter.getId(), null, true))).getField());
            assertEquals("name", assertThrows(BusinessException.class,
                    () -> callCenters.create(new CallCenter("Тест окна " + suffix, "Адрес", true))).getField());

            // колл-центр с операторами удалить нельзя
            assertThrows(BusinessException.class, () -> callCenters.delete(callCenter.getId()));

            // в неактивный колл-центр нельзя перевести, но оператора, который уже там, редактировать можно
            callCenter.setActive(false);
            callCenters.update(callCenter);
            operator.setFullName("Оператор окна 2");
            operators.update(operator);
            CallCenter inactive = callCenters.create(new CallCenter("Закрытый " + suffix, "Адрес", false));
            try {
                operator.setCallCenterId(inactive.getId());
                assertEquals("callCenter", assertThrows(BusinessException.class, () -> operators.update(operator)).getField());
            } finally {
                callCenters.delete(inactive.getId());
            }
            operator.setCallCenterId(callCenter.getId());

            // клиента с обращениями удалить нельзя; счётчики для окон считаются
            appeal = appeals.create(new Appeal(null, operator.getId(), client.getId(), null, null,
                    "Тема " + suffix, "TECH", AppealStatus.NEW, null, null));
            assertThrows(BusinessException.class, () -> clients.delete(client.getId()));
            assertEquals(1L, appeals.countByClient().get(client.getId()));
            assertEquals(1L, appeals.activeCountByOperator().get(operator.getId()));

            // оператора с обращением в работе удалить нельзя (CHECK в БД), а новые обращения становятся неназначенными
            Appeal working = appeals.create(new Appeal(null, operator.getId(), client.getId(), null, null,
                    "В работе " + suffix, "TECH", AppealStatus.NEW, null, null));
            appeals.changeStatus(working.getId(), AppealStatus.IN_PROGRESS);
            assertThrows(BusinessException.class, () -> operators.delete(operator.getId()));
            appeals.delete(working.getId());
            operators.delete(operator.getId());
            assertNull(appeals.getById(appeal.getId()).getOperatorId());
        } finally {
            if (appeal != null) appeals.delete(appeal.getId());
            if (operators.getAll().stream().anyMatch(o -> o.getId().equals(operator.getId()))) operators.delete(operator.getId());
            clients.delete(client.getId());
            callCenters.delete(callCenter.getId());
        }
    }
}
