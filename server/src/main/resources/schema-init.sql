-- AMC Portal Demo - DDL
-- 실행: psql -h <host> -p <port> -U <user> -d amc_portal -f schema-init.sql
-- NL2SQL API와 동일 DB이지만 portal 스키마로 분리해 충돌 방지

CREATE SCHEMA IF NOT EXISTS portal;

-- users
CREATE TABLE IF NOT EXISTS portal.users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    display_name   VARCHAR(100) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(30)  NOT NULL DEFAULT 'USER',
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_users_email ON portal.users (email);

-- boards (게시글)
CREATE TABLE IF NOT EXISTS portal.boards (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    author_id   BIGINT NOT NULL REFERENCES portal.users(id) ON DELETE CASCADE,
    view_count  BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_boards_created_at ON portal.boards (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_boards_author_id ON portal.boards (author_id);

-- comments (댓글)
CREATE TABLE IF NOT EXISTS portal.comments (
    id          BIGSERIAL PRIMARY KEY,
    board_id    BIGINT NOT NULL REFERENCES portal.boards(id) ON DELETE CASCADE,
    author_id   BIGINT NOT NULL REFERENCES portal.users(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_comments_board_id ON portal.comments (board_id);

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION portal.set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN SELECT unnest(ARRAY['users', 'boards', 'comments']) LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_%s_updated_at ON portal.%s;
            CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON portal.%s
                FOR EACH ROW EXECUTE FUNCTION portal.set_updated_at();
        ', t, t, t, t);
    END LOOP;
END$$;
