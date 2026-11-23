# Соответствие КР №3

Критерии взяты из текста задания, приложенного 21 ноября 2026 года. Здесь перечислены места реализации; результат фактической проверки отмечается отдельно в ACCEPTANCE_RESULT.txt и Maven surefire-reports.

| Критерий | Реализация | Как проверить |
|---|---|---|
| Тема прежней системы | Appeal, CallCenter, CallOperator; V1/V3/V4 | Старые обращения и операторы доступны в API |
| Controller/service/repository/domain/dto/security/filter/error/config | src/main/java/ru/mirea/callcenter | SQL не находится в UI |
| JWT + refresh session + rotation + logout | AuthService, JwtService, RefreshSession | login → refresh → logout → refresh 401 |
| BCrypt, принудительная смена временного пароля | PasswordPolicy, PasswordChangeRequiredFilter | После первого входа рабочий API даёт 403 до смены |
| Защита входа | LoginAttemptService | После пяти неверных паролей действует блокировка |
| 3 роли | VIEWER, EDITOR, ADMIN, @PreAuthorize | VIEWER не изменяет, EDITOR не управляет пользователями |
| UUID / отдельный номер | Appeal.id / externalId | POST и GET карточки |
| Текст, справочник, статус, число, boolean, теги | Appeal DTO / entity | Форма обращения |
| Вычисляемый показатель | PriorityCalculator | Изменение urgent/impact меняет priorityLevel |
| Даты/version/архив | AuditedVersionedEntity, Appeal | createdAt, updatedAt, version, archived в DTO |
| CRUD, архив и восстановление | AppealController/Service | DELETE архивирует, POST restore восстанавливает |
| Окончательное удаление ADMIN | permanentDelete, роль, подтверждающий header | EDITOR получает 403 |
| Конкурентное изменение | @Version + проверка DTO | Повтор PUT старой версии → 409 |
| Свободный поиск и все фильтры | SmartFilterParser, AppealSpecifications | center/status/urgency/priority/tag/archived/impact |
| Безопасные запросы | JPA Criteria / Specifications | Ввод не включается в SQL/JPQL как код |
| >=3 сортировки, серверные страницы, ограничение размера | PageableFactory | name/status/impact/updated, size 1..100 |
| Справочник: CRUD без удаления, active | CallCenterService | Изменение только ADMIN |
| Заметки: автор, время, pin, CRUD | NoteService, AppealNote | Редактирует автор или ADMIN; VIEWER читает |
| Bulk: UUID, архив/restore/status/tags, частичный результат | AppealService.bulk | Валидный + отсутствующий UUID дают успех и ошибку |
| CSV export с фильтром | CsvAppealService.export | В файле только q-выборка |
| CSV preview/apply, 2 режима, счётчики | CsvAppealService, ImportDtos | CREATE_ONLY и UPSERT_BY_EXTERNAL_ID |
| Аналитика: 8 показателей | AnalyticsService/Response | total/active/archived/urgent/avg/byCallCenter/byStatus/byPriority |
| Пользователи: список, страницы, создание, изменение, reset, disabled | UserAdminService | Выключенный пользователь не входит |
| Аудит и Correlation ID | AuditService, CorrelationIdFilter | ADMIN получает журнал, VIEWER — 403 |
| Bean Validation, DTO, ошибки HTTP | GlobalExceptionHandler | 400/401/403/404/409, нет stack trace в JSON |
| PostgreSQL + Flyway + validate | application.yml, V1..V5 | flyway_schema_history, проверка запуска |
| Ограничения и индексы | миграции | FK, unique, status/category/date checks, indexes |
| Docker: 2 сервиса, health, .env, localhost, volume | backend/docker-compose.yml | compose ps, down/up сохраняет данные |
| Swagger | OpenApiConfig, /swagger-ui.html | После входа ADMIN, спецификация доступна |
| Smoke/business/filter/security tests | src/test/java | mvn test или сборка Docker |
| Frontend | static/index.html, app.js, styles.css | Полный браузерный сценарий без JavaFX |
| Android-ready | NATIVE clientType + JSON DTO | Нет зависимости REST от браузерной сессии |

## Смысл показателей

«Активные» в аналитике означает неархивные записи, включая решённые. Активная нагрузка оператора — только неархивные NEW/IN_PROGRESS/ESCALATED. Приоритет и загрузка считаются сервером. Аналитика относится ко всем записям БД, экспорт — к текущему фильтру.

## Границы реализации

- КР2 сохраняется отдельным клиентом. Миграция данных однократная; изменения JavaFX после миграции не синхронизируются с новым API.
- Приоритет — учебное правило влияния/срочности, не машинное обучение.
- Docker-конфигурация рассчитана на локальную защиту. Для публичного размещения нужны HTTPS и COOKIE_SECURE=true.
- Access JWT остаётся действителен до срока истечения после logout; refresh-сессия отзывается немедленно, как требует задание. Отключение пользователя проверяется по БД при каждом запросе.


## Расширение: управление всеми четырьмя сущностями

Разделы «Клиенты» и «Операторы»: карточки, серверный поиск и страницы, создание, изменение с version и удаление без связанных обращений. VIEWER читает, EDITOR управляет контактами клиентов, ADMIN также управляет операторами и удалением. Сервер запрещает удалять историю, повторные телефоны/логины и устаревшие версии; изменения записываются в аудит. V5 переводит связь операторов на UUID-справочник center. Проверка: `scripts/verify_kr3.py`, группа client/operator CRUD; общий итог 19 успешных групп.
