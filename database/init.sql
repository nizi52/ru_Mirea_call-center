DROP TABLE IF EXISTS appeals;
DROP TABLE IF EXISTS operators;
DROP TABLE IF EXISTS clients;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id SERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(300),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE clients (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(150)
);

CREATE TABLE operators (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    login VARCHAR(50) NOT NULL UNIQUE,
    department_id INTEGER NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE appeals (
    id SERIAL PRIMARY KEY,
    operator_id INTEGER REFERENCES operators(id) ON DELETE SET NULL,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    topic VARCHAR(300) NOT NULL CHECK (length(trim(topic)) > 0),
    category VARCHAR(20) NOT NULL CHECK (category IN ('TECH','COMPLAINT','CONSULTATION','BILLING')),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','IN_PROGRESS','ESCALATED','RESOLVED','CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    CONSTRAINT valid_closed_at CHECK (closed_at IS NULL OR closed_at >= created_at),
    CONSTRAINT working_requires_operator CHECK (status = 'NEW' OR operator_id IS NOT NULL),
    CONSTRAINT closed_date_matches_status CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL))
);

INSERT INTO departments(name,description,is_active) VALUES
('Техподдержка','Техническая помощь клиентам',TRUE),('Продажи','Консультации и подключение услуг',TRUE),('Биллинг','Платежи и возвраты',TRUE),('Контроль качества','Работа с жалобами',FALSE);
INSERT INTO operators(full_name, login, department_id, is_active) VALUES
('Анна Иванова','aivanova',1,TRUE),('Пётр Смирнов','psmirnov',2,TRUE),('Мария Кузнецова','mkuznetsova',3,TRUE),('Илья Орлов','iorlov',1,TRUE),('Елена Волкова','evolkova',4,FALSE);
INSERT INTO clients(full_name,phone,email) VALUES
('Иван Петров','+79990000001','ivan@example.com'),('Ольга Соколова','+79990000002','olga@example.com'),('Денис Егоров','+79990000003','denis@example.com'),('Светлана Белова','+79990000004','svetlana@example.com'),('Артём Лебедев','+79990000005','artem@example.com'),('Кирилл Фёдоров','+79990000006','kirill@example.com'),('Наталья Романова','+79990000007','natalia@example.com'),('Роман Павлов','+79990000008','roman@example.com'),('Вера Котова','+79990000009','vera@example.com'),('Максим Гусев','+79990000010','maxim@example.com');
INSERT INTO appeals(operator_id,client_id,topic,category,status,created_at,closed_at) VALUES
(1,1,'Не работает интернет','TECH','IN_PROGRESS','2026-09-01 09:00',NULL),
(1,2,'Ошибка авторизации','TECH','ESCALATED','2026-09-02 10:00',NULL),
(2,3,'Тарифы для бизнеса','CONSULTATION','RESOLVED','2026-09-03 11:00',NULL),
(3,4,'Двойное списание','BILLING','CLOSED','2026-09-04 12:00','2026-09-04 14:30'),
(4,5,'Жалоба на обслуживание','COMPLAINT','NEW','2026-09-05 13:00',NULL),
(1,6,'Настройка роутера','TECH','CLOSED','2026-09-06 09:00','2026-09-06 10:10'),
(2,7,'Подключение услуги','CONSULTATION','NEW','2026-09-07 10:30',NULL),
(3,8,'Возврат платежа','BILLING','IN_PROGRESS','2026-09-08 11:40',NULL),
(4,9,'Сбои в приложении','TECH','RESOLVED','2026-09-09 12:50',NULL),
(2,10,'Условия скидки','CONSULTATION','CLOSED','2026-09-10 14:00','2026-09-10 15:00');
