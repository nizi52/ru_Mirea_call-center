-- Seed only on a completely empty predecessor database.
DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM call_centers) AND NOT EXISTS(SELECT 1 FROM clients) THEN
INSERT INTO call_centers(name,address,is_active) VALUES
('Центральный офис','Москва, ул. Тверская, 10',TRUE),('Северный филиал','Санкт-Петербург, Невский пр., 25',TRUE),('Южный филиал','Краснодар, ул. Красная, 50',TRUE),('Старый офис','Казань, ул. Баумана, 7',FALSE);
INSERT INTO operators(full_name, login, call_center_id, is_active) VALUES
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

END IF;
END $$;
INSERT INTO center(id,code,name,sla_hours,active,created_at,updated_at,version,legacy_id)
SELECT md5('center:'||id)::uuid,'CENTER-'||id,name,24,is_active,now(),now(),0,id FROM call_centers
WHERE NOT EXISTS(SELECT 1 FROM center WHERE legacy_id=call_centers.id);
INSERT INTO appeal(id,external_id,topic,client_name,category,email,phone,center_id,status,impact_score,urgent,priority_level,archived,created_at,updated_at,version,operator_id,legacy_client_id,closed_at)
SELECT md5('appeal:'||a.id)::uuid,'KR2-'||a.id,a.topic,c.full_name,a.category,c.email,c.phone,
 coalesce((SELECT id FROM center WHERE legacy_id=o.call_center_id),md5('center:unassigned')::uuid),a.status,50,false,
 CASE WHEN a.status IN ('RESOLVED','CLOSED') THEN 'NOT_APPLICABLE' WHEN a.status='ESCALATED' THEN 'HIGH' ELSE 'MEDIUM' END,
 false,a.created_at AT TIME ZONE 'Europe/Moscow',a.created_at AT TIME ZONE 'Europe/Moscow',0,a.operator_id,a.client_id,a.closed_at AT TIME ZONE 'Europe/Moscow'
FROM appeals a JOIN clients c ON c.id=a.client_id LEFT JOIN operators o ON o.id=a.operator_id
WHERE NOT EXISTS(SELECT 1 FROM appeal WHERE external_id='KR2-'||a.id);
