DROP TABLE IF EXISTS appeals;
DROP TABLE IF EXISTS operators;

CREATE TABLE operators (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    login VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE appeals (
    id SERIAL PRIMARY KEY,
    operator_id INTEGER REFERENCES operators(id) ON DELETE SET NULL,
    client_name VARCHAR(150) NOT NULL,
    client_phone VARCHAR(30) NOT NULL,
    topic VARCHAR(300) NOT NULL CHECK (length(trim(topic)) > 0),
    category VARCHAR(20) NOT NULL CHECK (category IN ('TECH','COMPLAINT','CONSULTATION','BILLING')),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','IN_PROGRESS','ESCALATED','RESOLVED','CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL CHECK (closed_at IS NULL OR closed_at >= created_at)
);

INSERT INTO operators(full_name, login, department, is_active) VALUES
('Анна Иванова','aivanova','Техподдержка',TRUE),('Пётр Смирнов','psmirnov','Продажи',TRUE),('Мария Кузнецова','mkuznetsova','Биллинг',TRUE),('Илья Орлов','iorlov','Техподдержка',TRUE),('Елена Волкова','evolkova','Контроль качества',FALSE);
INSERT INTO appeals(operator_id,client_name,client_phone,topic,category,status,created_at,closed_at) VALUES
(1,'Иван Петров','+79990000001','Не работает интернет','TECH','IN_PROGRESS','2026-09-01 09:00',NULL),
(1,'Ольга Соколова','+79990000002','Ошибка авторизации','TECH','ESCALATED','2026-09-02 10:00',NULL),
(2,'Денис Егоров','+79990000003','Тарифы для бизнеса','CONSULTATION','RESOLVED','2026-09-03 11:00',NULL),
(3,'Светлана Белова','+79990000004','Двойное списание','BILLING','CLOSED','2026-09-04 12:00','2026-09-04 14:30'),
(4,'Артём Лебедев','+79990000005','Жалоба на обслуживание','COMPLAINT','NEW','2026-09-05 13:00',NULL),
(1,'Кирилл Фёдоров','+79990000006','Настройка роутера','TECH','CLOSED','2026-09-06 09:00','2026-09-06 10:10'),
(2,'Наталья Романова','+79990000007','Подключение услуги','CONSULTATION','NEW','2026-09-07 10:30',NULL),
(3,'Роман Павлов','+79990000008','Возврат платежа','BILLING','IN_PROGRESS','2026-09-08 11:40',NULL),
(4,'Вера Котова','+79990000009','Сбои в приложении','TECH','RESOLVED','2026-09-09 12:50',NULL),
(2,'Максим Гусев','+79990000010','Условия скидки','CONSULTATION','CLOSED','2026-09-10 14:00','2026-09-10 15:00');
