# Информационная система колл-центра (КР №2, JavaFX)

Настольное приложение на JavaFX для учёта обращений клиентов. Это продолжение КР №1: те же сущности `Department`, `Operator`, `Client`, `Appeal`, те же бизнес-правила и та же база PostgreSQL `call_center`. Приложение подключается к базе напрямую через JDBC.

## Возможности

- таблица обращений (`TableView` + `ObservableList`), понятные заголовки, без технических id;
- добавление, редактирование, просмотр и удаление обращения (удаление только с подтверждением);
- клиент, оператор и категория выбираются из списков, идентификаторы вручную не вводятся;
- просмотр связанных данных: обращение + клиент + оператор с отделом;
- поиск по имени клиента, телефону и теме;
- фильтры: статус, категория, период дат создания; сортировка: сначала новые, сначала старые, по статусу (плюс сортировка по клику на заголовок столбца);
- смена статуса строго по цепочке `NEW → IN_PROGRESS → (ESCALATED) → RESOLVED → CLOSED`;
- статистика: всего обращений, не закрыто, среднее время до закрытия, доля эскалированных, разбивка по статусам, категориям и операторам. Считается по обращениям, показанным в таблице, то есть с учётом фильтров;
- экспорт текущей выборки в `.xlsx` или `.csv`, путь выбирается через `FileChooser`;
- валидация до сохранения: понятное сообщение и подсветка неверного поля.

## Требования

- Java 21, Maven 3.9+;
- PostgreSQL 16 (проще всего через Docker Desktop).

## Запуск

1. Поднять базу (создаётся `call_center`, `database/init.sql` выполняется при первом запуске):

   ```powershell
   docker compose up -d --wait
   ```

   Без Docker: создайте пустую БД `call_center` и выполните [database/init.sql](database/init.sql). Скрипт удаляет существующие таблицы проекта, поэтому используйте новую или демонстрационную БД.

2. Проверить параметры подключения в [src/main/resources/database.properties](src/main/resources/database.properties). Их можно переопределить переменными окружения `CALLCENTER_DB_URL`, `CALLCENTER_DB_USER`, `CALLCENTER_DB_PASSWORD`.

3. Запустить приложение:

   ```powershell
   mvn javafx:run
   ```

   либо `powershell -ExecutionPolicy Bypass -File .\run.ps1` (скрипт сам поднимет контейнер, если PostgreSQL не отвечает на `localhost:5432`).

Остановка базы: `docker compose down`. Данные лежат в volume `callcenter-pgdata`; не используйте `docker compose down -v`, он удаляет данные.

Другие команды: `mvn clean compile`, `mvn test`, `mvn clean package`.

## Тесты

`mvn test` запускает тесты без БД (статистика, цепочка статусов, экспорт, перевод ошибок БД). Интеграционный тест выполняется, только если задан `CALLCENTER_DB_URL`:

```powershell
$env:CALLCENTER_DB_URL = 'jdbc:postgresql://localhost:5432/call_center'
$env:CALLCENTER_DB_USER = 'postgres'
$env:CALLCENTER_DB_PASSWORD = 'postgres'
mvn test
```

Тест создаёт свои записи и удаляет их, но лучше запускать его на отдельной базе.

## Архитектура

```text
View (FXML + CSS) → Controller → Service → Repository (JDBC) → PostgreSQL
```

| Пакет | Назначение |
|---|---|
| `ui` | `App` (Stage/Scene), `AppContext` (сборка сервисов) |
| `ui.controller` | `MainController`, `AppealFormController`, `DetailsController`, `StatsController` |
| `ui.util` | `AlertUtil` (диалоги), `UiErrors` (текст ошибок), `Labels` (русские названия), `WindowFactory` (открытие окон) |
| `service` | бизнес-правила, поиск/фильтры/сортировка, статистика, экспорт |
| `repository` | параметризованные SQL-запросы (`PreparedStatement`) |
| `model` | сущности |
| `util` | `DatabaseManager`, `DbErrors`, экспортёры `.xlsx` и `.csv` |
| `resources/.../ui` | `*.fxml` и `style.css` |

SQL есть только в `repository`, обработчики кнопок его не содержат. Пароль БД хранится в `database.properties` или переменных окружения, а не в Java-коде.

## ER-диаграмма

```mermaid
erDiagram
    DEPARTMENTS ||--o{ OPERATORS : "содержит"
    CLIENTS ||--o{ APPEALS : "создает"
    OPERATORS ||--o{ APPEALS : "обрабатывает"
    DEPARTMENTS {
        int id PK
        varchar name UK
        varchar description
        boolean is_active
    }
    OPERATORS {
        int id PK
        varchar full_name
        varchar login UK
        int department_id FK
        boolean is_active
    }
    CLIENTS {
        int id PK
        varchar full_name
        varchar phone UK
        varchar email
    }
    APPEALS {
        int id PK
        int operator_id FK
        int client_id FK
        varchar topic
        varchar category
        varchar status
        timestamp created_at
        timestamp closed_at
    }
```

## Бизнес-правила

1. Клиент, тема (до 300 символов) и категория обязательны.
2. Назначаемый оператор и клиент должны существовать; новому обращению можно назначить только активного оператора.
3. Допустимые категории: `TECH`, `COMPLAINT`, `CONSULTATION`, `BILLING`.
4. Статусы меняются только вперёд по цепочке, без отката и прыжков.
5. Перевод в работу и закрытие невозможны без оператора; время закрытия ставится автоматически.
6. У оператора не больше пяти активных обращений (`NEW`, `IN_PROGRESS`, `ESCALATED`).

Краткая теория и сценарий демонстрации находятся в [DEFENSE.md](DEFENSE.md).
