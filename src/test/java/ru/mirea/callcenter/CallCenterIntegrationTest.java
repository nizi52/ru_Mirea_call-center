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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CallCenterIntegrationTest {
    @Test
    void fourEntitiesBusinessRulesSearchStatisticsAndExport() throws Exception {
        assumeTrue(System.getenv("CALLCENTER_DB_URL") != null, "Set CALLCENTER_DB_URL to an isolated test database");

        DepartmentService departments = new DepartmentService(new DepartmentRepository());
        ClientService clients = new ClientService(new ClientRepository());
        OperatorService operators = new OperatorService(new OperatorRepository(), departments);
        AppealService appeals = new AppealService(new AppealRepository(), operators, clients);
        String suffix = Long.toString(System.nanoTime());

        Department department = departments.create(new Department("Тест " + suffix, "Интеграционная проверка", true));
        Client client = clients.create(new Client("Тестовый клиент", "+7" + suffix, "test@example.org"));
        Operator operator = operators.create(new Operator(null, "Тестовый оператор", "test" + suffix, department.getId(), null, true));
        Appeal appeal = null;
        try {
            assertEquals(department.getId(), operators.getById(operator.getId()).getDepartmentId());
            assertEquals(client.getPhone(), clients.getById(client.getId()).getPhone());
            client.setFullName("Обновлённый клиент");
            clients.update(client);
            department.setDescription("Обновлённое описание");
            departments.update(department);
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
            assertFalse(appeals.searchClient("Обновлённый клиент").isEmpty());
            assertFalse(appeals.searchTopic(suffix).isEmpty());
            assertTrue(appeals.filterByStatus(AppealStatus.NEW).stream().anyMatch(a -> a.getId().equals(appealId)));
            assertTrue(appeals.filterByCreatedRange(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
                    .stream().anyMatch(a -> a.getId().equals(appealId)));
            assertFalse(appeals.sortByCreatedAt().isEmpty());
            assertFalse(appeals.sortByStatus().isEmpty());

            appeals.changeStatus(appeal.getId(), AppealStatus.IN_PROGRESS);
            appeals.changeStatus(appeal.getId(), AppealStatus.ESCALATED);
            appeals.changeStatus(appeal.getId(), AppealStatus.RESOLVED);
            appeals.changeStatus(appeal.getId(), AppealStatus.CLOSED);
            assertNotNull(appeals.getById(appeal.getId()).getClosedAt());
            assertTrue(appeals.statistics().contains("Всего обращений"));

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
            departments.delete(department.getId());
        }
    }
}
