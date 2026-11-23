CREATE TABLE call_centers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    address VARCHAR(200) NOT NULL,
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
    call_center_id INTEGER NOT NULL REFERENCES call_centers(id) ON DELETE RESTRICT,
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

