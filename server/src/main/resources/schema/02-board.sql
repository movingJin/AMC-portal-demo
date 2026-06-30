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
    id              BIGSERIAL    PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    content         TEXT         NOT NULL,
    board_master_id BIGINT       REFERENCES portal.board_masters(id),
    view_count      BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      BIGINT       NOT NULL REFERENCES portal.users(id) ON DELETE CASCADE,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      BIGINT       REFERENCES portal.users(id),
    deleted_at      TIMESTAMPTZ  DEFAULT NULL,
    deleted_by      BIGINT       REFERENCES portal.users(id)
);
CREATE INDEX IF NOT EXISTS idx_boards_created_at      ON portal.boards (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_boards_created_by      ON portal.boards (created_by);
CREATE INDEX IF NOT EXISTS idx_boards_board_master_id ON portal.boards (board_master_id);

-- board_files (첨부파일)
CREATE TABLE IF NOT EXISTS portal.board_files (
    id              BIGSERIAL    PRIMARY KEY,
    board_id        BIGINT       NOT NULL REFERENCES portal.boards(id) ON DELETE CASCADE,
    original_name   VARCHAR(255) NOT NULL,
    stored_name     VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size       BIGINT       NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_board_files_board_id ON portal.board_files (board_id);

-- board_file_history (첨부파일 업로드/삭제 이력)
CREATE TABLE IF NOT EXISTS portal.board_file_history (
    id              BIGSERIAL    PRIMARY KEY,
    file_id         BIGINT       NOT NULL,
    board_id        BIGINT       NOT NULL REFERENCES portal.boards(id),
    event_type      VARCHAR(10)  NOT NULL CHECK (event_type IN ('UPLOAD', 'DELETE')),
    original_name   VARCHAR(255) NOT NULL,
    stored_name     VARCHAR(255) NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    file_size       BIGINT       NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    acted_by        BIGINT       NOT NULL REFERENCES portal.users(id),
    acted_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_board_file_history_file_id  ON portal.board_file_history (file_id);
CREATE INDEX IF NOT EXISTS idx_board_file_history_board_id ON portal.board_file_history (board_id);

-- board_file_downloads (첨부파일 다운로드 이력)
CREATE TABLE IF NOT EXISTS portal.board_file_downloads (
    id              BIGSERIAL    PRIMARY KEY,
    file_id         BIGINT       NOT NULL,
    board_id        BIGINT       NOT NULL REFERENCES portal.boards(id),
    original_name   VARCHAR(255) NOT NULL,
    stored_name     VARCHAR(255) NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    user_id         BIGINT       NOT NULL REFERENCES portal.users(id),
    ip_address      VARCHAR(45),
    downloaded_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_board_file_downloads_file_id  ON portal.board_file_downloads (file_id);
CREATE INDEX IF NOT EXISTS idx_board_file_downloads_board_id ON portal.board_file_downloads (board_id);
CREATE INDEX IF NOT EXISTS idx_board_file_downloads_user_id  ON portal.board_file_downloads (user_id);

-- comments (댓글/대댓글)
CREATE TABLE IF NOT EXISTS portal.comments (
    id          BIGSERIAL   PRIMARY KEY,
    board_id    BIGINT      NOT NULL REFERENCES portal.boards(id) ON DELETE CASCADE,
    parent_id   BIGINT      REFERENCES portal.comments(id) ON DELETE CASCADE,
    created_by  BIGINT      NOT NULL REFERENCES portal.users(id) ON DELETE CASCADE,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by  BIGINT      REFERENCES portal.users(id)
);
CREATE INDEX IF NOT EXISTS idx_comments_board_id  ON portal.comments (board_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON portal.comments (parent_id);
