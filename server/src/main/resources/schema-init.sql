-- AMC Portal Demo - DDL (진입점)
-- 실행: resources/ 디렉토리에서 psql -h <host> -p <port> -U <user> -d amc_portal -f schema-init.sql
-- NL2SQL API와 동일 DB이지만 portal 스키마로 분리해 충돌 방지
--
-- 파일 구조:
--   schema/01-users.sql          사용자 (users)
--   schema/02-board.sql          게시판 (board_masters, boards, board_files, board_file_history, board_file_downloads, comments)
--   schema/03-projects.sql       프로젝트 (projects)
--   schema/99-triggers.sql       updated_at 자동 갱신 트리거

CREATE SCHEMA IF NOT EXISTS portal;

\i schema/01-users.sql
\i schema/02-board.sql
\i schema/03-projects.sql
\i schema/99-triggers.sql
