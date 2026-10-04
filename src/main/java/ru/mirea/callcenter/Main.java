package ru.mirea.callcenter;

import ru.mirea.callcenter.exception.BusinessException;
import ru.mirea.callcenter.exception.EntityNotFoundException;
import ru.mirea.callcenter.model.*;
import ru.mirea.callcenter.repository.*;
import ru.mirea.callcenter.service.*;
import ru.mirea.callcenter.util.CsvExporter;
import ru.mirea.callcenter.util.ExcelExporter;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    private final Scanner scanner = new Scanner(System.in);
    private final DepartmentService departments = new DepartmentService(new DepartmentRepository());
    private final ClientService clients = new ClientService(new ClientRepository());
    private final OperatorService operators = new OperatorService(new OperatorRepository(), departments);
    private final AppealService appeals = new AppealService(new AppealRepository(), operators, clients);

    public static void main(String[] args) { new Main().run(); }

    @FunctionalInterface
    private interface MenuAction { void run(int choice) throws Exception; }

    private void menu(String title, String options, MenuAction action) {
        while (true) {
            System.out.println("\n" + title + "\n" + options);
            int choice = readInt("Действие: ");
            if (choice == 0) return;
            try {
                action.run(choice);
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Ошибка базы данных: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void run() {
        menu("===== СИСТЕМА КОЛЛ-ЦЕНТРА =====",
                "1. Операторы\n2. Клиенты\n3. Отделы\n4. Обращения\n5. Поиск обращений\n" +
                "6. Фильтрация и сортировка\n7. Статистика\n8. Экспорт данных\n9. Вывести таблицы базы данных\n0. Выход",
                choice -> {
                    switch (choice) {
                        case 1 -> operatorMenu();
                        case 2 -> clientMenu();
                        case 3 -> departmentMenu();
                        case 4 -> appealMenu();
                        case 5 -> searchMenu();
                        case 6 -> filterSortMenu();
                        case 7 -> System.out.println(appeals.statistics());
                        case 8 -> export();
                        case 9 -> showTables();
                        default -> System.out.println("Нет такого пункта меню.");
                    }
                });
        System.out.println("До свидания!");
    }

    private void departmentMenu() {
        menu("ОТДЕЛЫ", "1. Создать\n2. Список\n3. По ID\n4. Изменить\n5. Удалить\n0. Назад", choice -> {
            switch (choice) {
                case 1 -> System.out.println("Создан отдел ID " + departments.create(readDepartment(null)).getId());
                case 2 -> print(departments.getAll());
                case 3 -> System.out.println(departments.getById(readInt("ID: ")));
                case 4 -> {
                    Department old = departments.getById(readInt("ID: "));
                    departments.update(readDepartment(old));
                    System.out.println("Отдел обновлён.");
                }
                case 5 -> { departments.delete(readInt("ID: ")); System.out.println("Отдел удалён."); }
                default -> System.out.println("Нет такого пункта.");
            }
        });
    }

    private void clientMenu() {
        menu("КЛИЕНТЫ", "1. Создать\n2. Список\n3. По ID\n4. Изменить\n5. Удалить\n0. Назад", choice -> {
            switch (choice) {
                case 1 -> System.out.println("Создан клиент ID " + clients.create(readClient(null)).getId());
                case 2 -> print(clients.getAll());
                case 3 -> System.out.println(clients.getById(readInt("ID: ")));
                case 4 -> {
                    Client old = clients.getById(readInt("ID: "));
                    clients.update(readClient(old));
                    System.out.println("Клиент обновлён.");
                }
                case 5 -> { clients.delete(readInt("ID: ")); System.out.println("Клиент удалён."); }
                default -> System.out.println("Нет такого пункта.");
            }
        });
    }

    private void operatorMenu() {
        menu("ОПЕРАТОРЫ", "1. Создать\n2. Список\n3. По ID\n4. Изменить\n5. Удалить\n0. Назад", choice -> {
            switch (choice) {
                case 1 -> System.out.println("Создан оператор ID " + operators.create(readOperator(null)).getId());
                case 2 -> print(operators.getAll());
                case 3 -> System.out.println(operators.getById(readInt("ID: ")));
                case 4 -> {
                    Operator old = operators.getById(readInt("ID: "));
                    operators.update(readOperator(old));
                    System.out.println("Оператор обновлён.");
                }
                case 5 -> { operators.delete(readInt("ID: ")); System.out.println("Оператор удалён."); }
                default -> System.out.println("Нет такого пункта.");
            }
        });
    }

    private void appealMenu() {
        menu("ОБРАЩЕНИЯ", "1. Создать\n2. Список\n3. По ID\n4. Изменить\n5. Сменить статус\n6. Удалить\n0. Назад", choice -> {
            switch (choice) {
                case 1 -> System.out.println("Создано обращение ID " + appeals.create(readAppeal(null)).getId());
                case 2 -> print(appeals.getAll());
                case 3 -> System.out.println(appeals.getById(readInt("ID: ")));
                case 4 -> {
                    Appeal old = appeals.getById(readInt("ID: "));
                    appeals.updateDetails(readAppeal(old));
                    System.out.println("Обращение обновлено.");
                }
                case 5 -> {
                    appeals.changeStatus(readInt("ID: "), readStatus());
                    System.out.println("Статус обновлён.");
                }
                case 6 -> { appeals.delete(readInt("ID: ")); System.out.println("Обращение удалено."); }
                default -> System.out.println("Нет такого пункта.");
            }
        });
    }

    private void searchMenu() {
        menu("ПОИСК ОБРАЩЕНИЙ", "1. По имени/телефону клиента\n2. По теме\n0. Назад", choice -> {
            switch (choice) {
                case 1 -> print(appeals.searchClient(readRequired("Запрос: ")));
                case 2 -> print(appeals.searchTopic(readRequired("Запрос: ")));
                default -> System.out.println("Нет такого пункта.");
            }
        });
    }

    private void filterSortMenu() {
        menu("ФИЛЬТРАЦИЯ И СОРТИРОВКА", "1. По статусу\n2. По диапазону дат\n3. По дате создания\n4. По статусу (сортировка)\n0. Назад", choice -> {
            switch (choice) {
                case 1 -> print(appeals.filterByStatus(readStatus()));
                case 2 -> {
                    LocalDate from = readDate("Дата от (yyyy-MM-dd): ");
                    LocalDate to = readDate("Дата до (yyyy-MM-dd): ");
                    if (from.isAfter(to)) throw new BusinessException("Начальная дата позже конечной.");
                    print(appeals.filterByCreatedRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay().minusNanos(1)));
                }
                case 3 -> print(appeals.sortByCreatedAt());
                case 4 -> print(appeals.sortByStatus());
                default -> System.out.println("Нет такого пункта.");
            }
        });
    }

    private void export() throws Exception {
        List<Appeal> data = appeals.getAll();
        Path directory = Path.of("exports");
        new ExcelExporter().export(data, directory.resolve("appeals.xlsx"));
        new CsvExporter().export(data, directory.resolve("appeals.csv"));
        System.out.println("Экспортированы exports/appeals.xlsx и exports/appeals.csv");
    }

    private void showTables() throws SQLException {
        System.out.println("\nОТДЕЛЫ"); print(departments.getAll());
        System.out.println("\nОПЕРАТОРЫ"); print(operators.getAll());
        System.out.println("\nКЛИЕНТЫ"); print(clients.getAll());
        System.out.println("\nОБРАЩЕНИЯ"); print(appeals.getAll());
    }

    private Department readDepartment(Department old) {
        String name = readDefault("Название", old == null ? null : old.getName());
        String description = readDefault("Описание", old == null ? null : old.getDescription());
        boolean active = readBoolean("Активен", old == null || old.isActive());
        return new Department(old == null ? null : old.getId(), name, description, active);
    }

    private Client readClient(Client old) {
        String name = readDefault("ФИО", old == null ? null : old.getFullName());
        String phone = readDefault("Телефон", old == null ? null : old.getPhone());
        String email = readDefault("Email", old == null ? null : old.getEmail());
        return new Client(old == null ? null : old.getId(), name, phone, email);
    }

    private Operator readOperator(Operator old) {
        String name = readDefault("ФИО", old == null ? null : old.getFullName());
        String login = readDefault("Логин", old == null ? null : old.getLogin());
        Integer departmentId = readOptionalInt("ID отдела", old == null ? null : old.getDepartmentId(), false);
        boolean active = readBoolean("Активен", old == null || old.isActive());
        return new Operator(old == null ? null : old.getId(), name, login, departmentId, null, active);
    }

    private Appeal readAppeal(Appeal old) {
        Integer clientId = readOptionalInt("ID клиента", old == null ? null : old.getClientId(), false);
        Integer operatorId = readOptionalInt("ID оператора", old == null ? null : old.getOperatorId(), true);
        String topic = readDefault("Тема", old == null ? null : old.getTopic());
        String category = readDefault("Категория TECH/COMPLAINT/CONSULTATION/BILLING", old == null ? null : old.getCategory());
        return new Appeal(old == null ? null : old.getId(), operatorId, clientId, null, null,
                topic, category == null ? null : category.toUpperCase(),
                old == null ? AppealStatus.NEW : old.getStatus(),
                old == null ? null : old.getCreatedAt(), old == null ? null : old.getClosedAt());
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Ошибка: требуется целое число."); }
        }
    }

    private Integer readOptionalInt(String prompt, Integer current, boolean nullable) {
        while (true) {
            System.out.print(prompt + (current == null ? ": " : " [" + current + "]: "));
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) return current;
            if (nullable && value.equals("-")) return null;
            try { return Integer.valueOf(value); }
            catch (NumberFormatException e) { System.out.println("Ошибка: требуется целое число. '-' снимает назначение оператора."); }
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("Поле не может быть пустым.");
        }
    }

    private String readDefault(String name, String current) {
        System.out.print(name + (current == null ? ": " : " [" + current + "]: "));
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? current : value;
    }

    private boolean readBoolean(String prompt, boolean current) {
        while (true) {
            System.out.print(prompt + " [" + current + "]: ");
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) return current;
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return Boolean.parseBoolean(value);
            System.out.println("Введите true или false.");
        }
    }

    private AppealStatus readStatus() {
        while (true) {
            System.out.print("Статус " + Arrays.toString(AppealStatus.values()) + ": ");
            try { return AppealStatus.valueOf(scanner.nextLine().trim().toUpperCase()); }
            catch (IllegalArgumentException e) { System.out.println("Некорректный статус."); }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            try { return LocalDate.parse(readRequired(prompt)); }
            catch (java.time.format.DateTimeParseException e) { System.out.println("Введите дату в формате yyyy-MM-dd."); }
        }
    }

    private void print(List<?> items) {
        if (items.isEmpty()) System.out.println("Записей нет.");
        else items.forEach(System.out::println);
    }
}
