ALTER TABLE clients ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE operators ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE operators ADD COLUMN center_id UUID REFERENCES center(id) ON DELETE RESTRICT;
UPDATE operators o SET center_id=c.id FROM center c WHERE c.legacy_id=o.call_center_id;
ALTER TABLE operators ALTER COLUMN center_id SET NOT NULL;
ALTER TABLE operators ALTER COLUMN call_center_id DROP NOT NULL;
CREATE INDEX operators_center_idx ON operators(center_id);
