-- users
CREATE TABLE IF NOT EXISTS portal.users (
    id             BIGSERIAL PRIMARY KEY,
    keycloak_id    VARCHAR(64)  UNIQUE,
    email          VARCHAR(255) NOT NULL UNIQUE,
    display_name   VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255),
    role           VARCHAR(30)  NOT NULL DEFAULT 'USER',
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     BIGINT       REFERENCES portal.users(id)
);
CREATE INDEX IF NOT EXISTS idx_users_email        ON portal.users (email);
CREATE INDEX IF NOT EXISTS idx_users_keycloak_id  ON portal.users (keycloak_id);
