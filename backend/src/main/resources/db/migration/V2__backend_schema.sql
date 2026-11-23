CREATE TABLE app_user (
                          id UUID PRIMARY KEY,
                          email VARCHAR(254) NOT NULL UNIQUE,
                          display_name VARCHAR(160) NOT NULL,
                          password_hash VARCHAR(100) NOT NULL,
                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                          password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
                          failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                          locked_until TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
                               user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                               role VARCHAR(32) NOT NULL,
                               PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_session (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                 token_hash VARCHAR(64) NOT NULL UNIQUE,
                                 expires_at TIMESTAMPTZ NOT NULL,
                                 revoked_at TIMESTAMPTZ,
                                 replaced_by UUID,
                                 user_agent VARCHAR(500),
                                 ip_address VARCHAR(64),
                                 created_at TIMESTAMPTZ NOT NULL,
                                 last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user
    ON refresh_session(user_id);

CREATE INDEX idx_refresh_expires
    ON refresh_session(expires_at);


CREATE TABLE center (
                             id UUID PRIMARY KEY,
                             code VARCHAR(50) NOT NULL UNIQUE,
                             name VARCHAR(200),
                             sla_hours INTEGER,
                             active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMPTZ NOT NULL,
                             updated_at TIMESTAMPTZ NOT NULL,
                             version BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE appeal (
                         id UUID PRIMARY KEY,
                         external_id VARCHAR(100) UNIQUE,
                         topic VARCHAR(300) NOT NULL,
                         client_name VARCHAR(150) NOT NULL,
                         category VARCHAR(100),
                         email VARCHAR(254),
                         phone VARCHAR(32),
                         due_date DATE,
                         center_id UUID REFERENCES center(id),
                         status VARCHAR(32) NOT NULL,

    -- Влияние по 100-балльной шкале.
                         impact_score NUMERIC(5,2),

                         urgent BOOLEAN NOT NULL DEFAULT FALSE,
                         priority_level VARCHAR(32) NOT NULL,
                         archived BOOLEAN NOT NULL DEFAULT FALSE,
                         archived_at TIMESTAMPTZ,
                         archived_by UUID REFERENCES app_user(id),
                         created_by UUID REFERENCES app_user(id),
                         updated_by UUID REFERENCES app_user(id),
                         created_at TIMESTAMPTZ NOT NULL,
                         updated_at TIMESTAMPTZ NOT NULL,
                         version BIGINT NOT NULL DEFAULT 0,

                         CONSTRAINT chk_appeal_impact_score
                             CHECK (
                                 impact_score IS NULL
                                     OR (impact_score >= 0.00 AND impact_score <= 100.00)
                                 )
);

COMMENT ON COLUMN appeal.impact_score
    IS 'Влияние обращения по 100-балльной шкале (0..100)';

CREATE INDEX idx_appeal_name
    ON appeal(client_name, topic, category);

CREATE INDEX idx_appeal_center
    ON appeal(center_id);

CREATE INDEX idx_appeal_status
    ON appeal(status);

CREATE INDEX idx_appeal_archived
    ON appeal(archived);

CREATE INDEX idx_appeal_priority
    ON appeal(priority_level);


CREATE TABLE appeal_tag (
                             appeal_id UUID NOT NULL REFERENCES appeal(id) ON DELETE CASCADE,
                             tag VARCHAR(50) NOT NULL,
                             PRIMARY KEY (appeal_id, tag)
);

CREATE INDEX idx_appeal_tag_value
    ON appeal_tag(tag);


CREATE TABLE appeal_note (
                              id UUID PRIMARY KEY,
                              appeal_id UUID NOT NULL REFERENCES appeal(id) ON DELETE CASCADE,
                              author_id UUID NOT NULL REFERENCES app_user(id),
                              body VARCHAR(4000) NOT NULL,
                              pinned BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMPTZ NOT NULL,
                              updated_at TIMESTAMPTZ NOT NULL,
                              version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_note_appeal
    ON appeal_note(appeal_id, pinned DESC, created_at DESC);


CREATE TABLE audit_event (
                             id UUID PRIMARY KEY,
                             actor_id UUID,
                             actor_email VARCHAR(254),
                             event_type VARCHAR(80) NOT NULL,
                             entity_type VARCHAR(80),
                             entity_id UUID,
                             summary VARCHAR(1000) NOT NULL,
                             correlation_id VARCHAR(100),
                             ip_address VARCHAR(64),
                             user_agent VARCHAR(500),
                             occurred_at TIMESTAMPTZ NOT NULL,
                             details_json TEXT
);

CREATE INDEX idx_audit_occurred
    ON audit_event(occurred_at DESC);

CREATE INDEX idx_audit_actor
    ON audit_event(actor_email);

CREATE INDEX idx_audit_entity
    ON audit_event(entity_type, entity_id);
