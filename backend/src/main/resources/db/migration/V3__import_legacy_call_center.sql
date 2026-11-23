-- Keep KR1/KR2 tables; new API uses UUIDs and its own Flyway-managed schema.
ALTER TABLE appeal ADD COLUMN operator_id INTEGER REFERENCES operators(id) ON DELETE RESTRICT;
ALTER TABLE appeal ADD COLUMN legacy_client_id INTEGER REFERENCES clients(id) ON DELETE RESTRICT;
ALTER TABLE appeal ADD COLUMN closed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE center ADD COLUMN legacy_id INTEGER UNIQUE REFERENCES call_centers(id);
INSERT INTO center(id,code,name,sla_hours,active,created_at,updated_at,version,legacy_id)
SELECT md5('center:'||id)::uuid,'CENTER-'||id,name,24,is_active,now(),now(),0,id FROM call_centers;
INSERT INTO appeal(id,external_id,topic,client_name,category,email,phone,center_id,status,impact_score,urgent,priority_level,archived,created_at,updated_at,version,operator_id,legacy_client_id,closed_at)
SELECT md5('appeal:'||a.id)::uuid,'KR2-'||a.id,a.topic,c.full_name,a.category,c.email,c.phone,
 (SELECT id FROM center WHERE legacy_id=o.call_center_id),a.status,50,false,
 CASE WHEN a.status IN ('RESOLVED','CLOSED') THEN 'NOT_APPLICABLE' WHEN a.status='ESCALATED' THEN 'HIGH' ELSE 'MEDIUM' END,
 false,a.created_at AT TIME ZONE 'Europe/Moscow',a.created_at AT TIME ZONE 'Europe/Moscow',0,a.operator_id,a.client_id,a.closed_at AT TIME ZONE 'Europe/Moscow'
FROM appeals a JOIN clients c ON c.id=a.client_id LEFT JOIN operators o ON o.id=a.operator_id;
-- Unassigned legacy NEW appeals receive a center for subsequent routing.
INSERT INTO center(id,code,name,sla_hours,active,created_at,updated_at,version)
VALUES (md5('center:unassigned')::uuid,'GENERAL','Общий колл-центр',24,true,now(),now(),0);
UPDATE appeal SET center_id=md5('center:unassigned')::uuid WHERE center_id IS NULL;
ALTER TABLE appeal ALTER COLUMN center_id SET NOT NULL;
ALTER TABLE appeal ALTER COLUMN external_id SET NOT NULL;
ALTER TABLE appeal ADD CONSTRAINT appeal_status_check CHECK(status IN ('NEW','IN_PROGRESS','ESCALATED','RESOLVED','CLOSED'));
ALTER TABLE appeal ADD CONSTRAINT appeal_category_check CHECK(category IN ('TECH','COMPLAINT','CONSULTATION','BILLING'));
ALTER TABLE appeal ADD CONSTRAINT appeal_operator_check CHECK(status='NEW' OR operator_id IS NOT NULL);
ALTER TABLE appeal ADD CONSTRAINT appeal_closed_check CHECK((status='CLOSED')=(closed_at IS NOT NULL));
ALTER TABLE appeal ADD CONSTRAINT appeal_time_check CHECK(closed_at IS NULL OR closed_at>=created_at);
CREATE UNIQUE INDEX appeal_external_lower_idx ON appeal(lower(external_id));
CREATE INDEX appeal_phone_idx ON appeal(phone);
CREATE INDEX appeal_operator_idx ON appeal(operator_id);
