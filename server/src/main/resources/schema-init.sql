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
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     BIGINT       REFERENCES portal.users(id)
);
CREATE INDEX IF NOT EXISTS idx_users_email ON portal.users (email);

-- board_masters (게시판 관리)
CREATE TABLE IF NOT EXISTS portal.board_masters (
    id              BIGSERIAL     PRIMARY KEY,
    title           VARCHAR(100)  NOT NULL,
    description     VARCHAR(500),
    type            VARCHAR(20)   NOT NULL DEFAULT 'GENERAL'
        CHECK (type IN ('GENERAL', 'BLOG', 'GUESTBOOK')),
    file_yn         BOOLEAN       NOT NULL DEFAULT FALSE,
    file_max_count  INTEGER       NOT NULL DEFAULT 0,
    comment_yn      BOOLEAN       NOT NULL DEFAULT FALSE,
    use_yn          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      BIGINT        NOT NULL REFERENCES portal.users(id),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      BIGINT        REFERENCES portal.users(id)
);
CREATE INDEX IF NOT EXISTS idx_board_masters_type ON portal.board_masters (type);

-- boards (게시글)
CREATE TABLE IF NOT EXISTS portal.boards (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    board_master_id BIGINT REFERENCES portal.board_masters(id),
    view_count      BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      BIGINT NOT NULL REFERENCES portal.users(id) ON DELETE CASCADE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      BIGINT      REFERENCES portal.users(id),
    deleted_at      TIMESTAMPTZ DEFAULT NULL,
    deleted_by      BIGINT      REFERENCES portal.users(id)
);
-- 기존 DB에 컬럼 추가 (이미 테이블이 존재하는 경우)
CREATE INDEX IF NOT EXISTS idx_boards_created_at ON portal.boards (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_boards_created_by ON portal.boards (created_by);
CREATE INDEX IF NOT EXISTS idx_boards_board_master_id ON portal.boards (board_master_id);

-- board_files (첨부파일)
CREATE TABLE IF NOT EXISTS portal.board_files (
    id              BIGSERIAL     PRIMARY KEY,
    board_id        BIGINT        NOT NULL REFERENCES portal.boards(id) ON DELETE CASCADE,
    original_name   VARCHAR(255)  NOT NULL,
    stored_name     VARCHAR(255)  NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    storage_path    VARCHAR(500)  NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_board_files_board_id ON portal.board_files (board_id);

-- board_file_history (첨부파일 업로드/삭제 이력)
CREATE TABLE IF NOT EXISTS portal.board_file_history (
    id              BIGSERIAL     PRIMARY KEY,
    file_id         BIGINT        NOT NULL,
    board_id        BIGINT        NOT NULL REFERENCES portal.boards(id),
    event_type      VARCHAR(10)   NOT NULL CHECK (event_type IN ('UPLOAD', 'DELETE')),
    original_name   VARCHAR(255)  NOT NULL,
    stored_name     VARCHAR(255)  NOT NULL,
    storage_path    VARCHAR(500)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    content_type    VARCHAR(100)  NOT NULL,
    acted_by        BIGINT        NOT NULL REFERENCES portal.users(id),
    acted_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_board_file_history_file_id  ON portal.board_file_history (file_id);
CREATE INDEX IF NOT EXISTS idx_board_file_history_board_id ON portal.board_file_history (board_id);

-- board_file_downloads (첨부파일 다운로드 이력)
CREATE TABLE IF NOT EXISTS portal.board_file_downloads (
    id              BIGSERIAL     PRIMARY KEY,
    file_id         BIGINT        NOT NULL,
    board_id        BIGINT        NOT NULL REFERENCES portal.boards(id),
    original_name   VARCHAR(255)  NOT NULL,
    stored_name     VARCHAR(255)  NOT NULL,
    storage_path    VARCHAR(500)  NOT NULL,
    user_id         BIGINT        NOT NULL REFERENCES portal.users(id),
    ip_address      VARCHAR(45),
    downloaded_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_board_file_downloads_file_id  ON portal.board_file_downloads (file_id);
CREATE INDEX IF NOT EXISTS idx_board_file_downloads_board_id ON portal.board_file_downloads (board_id);
CREATE INDEX IF NOT EXISTS idx_board_file_downloads_user_id  ON portal.board_file_downloads (user_id);

-- comments (댓글)
CREATE TABLE IF NOT EXISTS portal.comments (
    id          BIGSERIAL PRIMARY KEY,
    board_id    BIGINT NOT NULL REFERENCES portal.boards(id) ON DELETE CASCADE,
    created_by  BIGINT NOT NULL REFERENCES portal.users(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT      REFERENCES portal.users(id)
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
    FOR t IN SELECT unnest(ARRAY['users', 'board_masters', 'boards', 'comments']) LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_%s_updated_at ON portal.%s;
            CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON portal.%s
                FOR EACH ROW EXECUTE FUNCTION portal.set_updated_at();
        ', t, t, t, t);
    END LOOP;
END$$;
