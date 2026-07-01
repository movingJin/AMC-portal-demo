-- projects (연구 프로젝트)
CREATE TABLE IF NOT EXISTS portal.projects (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    owner_id     BIGINT       NOT NULL REFERENCES portal.users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   BIGINT       NOT NULL REFERENCES portal.users(id),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by   BIGINT       REFERENCES portal.users(id)
);
CREATE INDEX IF NOT EXISTS idx_projects_owner_id ON portal.projects (owner_id);

-- project_members (연구 프로젝트 멤버)
CREATE TABLE IF NOT EXISTS portal.project_members (
    id          BIGSERIAL    PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES portal.projects(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES portal.users(id)    ON DELETE CASCADE,
    role        VARCHAR(30)  NOT NULL DEFAULT 'VIEWER'
        CHECK (role IN ('ADMIN', 'CONTRIBUTOR', 'VIEWER')),
    joined_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (project_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON portal.project_members (project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id    ON portal.project_members (user_id);

-- project_member_history (멤버 추가/삭제 이력)
CREATE TABLE IF NOT EXISTS portal.project_member_history (
    id          BIGSERIAL    PRIMARY KEY,
    project_id  BIGINT       NOT NULL REFERENCES portal.projects(id),
    user_id     BIGINT       NOT NULL REFERENCES portal.users(id),
    role        VARCHAR(30)  NOT NULL,
    event_type  VARCHAR(20)  NOT NULL CHECK (event_type IN ('JOINED', 'REMOVED', 'ROLE_CHANGED')),
    acted_by    BIGINT       NOT NULL REFERENCES portal.users(id),
    acted_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_project_member_history_project_id ON portal.project_member_history (project_id);
CREATE INDEX IF NOT EXISTS idx_project_member_history_user_id    ON portal.project_member_history (user_id);
