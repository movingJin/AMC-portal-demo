# AMC Portal Demo

서울아산병원(AMC) 데이터플랫폼 포털 서비스 **데모**.
게시판형 UI + NL2SQL 챗봇 UI를 단일 포털로 제공.

## 디렉터리

- [server/](server/) — Spring Boot 3.5.14 (Java 21) 백엔드
- [client/](client/) — Node.js 프론트엔드 (프레임워크 미정)

## 기술 스택 (확정)

| 영역 | 선택 |
|---|---|
| Backend | Spring Boot 3.5.14, Java 21, Gradle (Groovy DSL), Lombok |
| DB Access | Spring Data JPA + QueryDSL |
| DB | PostgreSQL |
| 인증 | Spring Security + JWT |
| Frontend | **Vite + React 19 (SPA)** + React Router, Grid + Chart 컴포넌트 포함. **Next.js 등 풀스택 메타프레임워크 금지** — 백엔드가 별도이므로 |
| LLM | **Azure OpenAI** (function calling으로 NL→SQL) |
| 캐시/세션 | **Redis** (JWT refresh token / 블랙리스트 / 세션) |
| 메일 | SMTP (회원가입 인증, 비밀번호 찾기) |
| 시크릿 | `server/.env` (커밋 금지) |

사용자가 직접 지정한 스택이다. 임의로 변경하거나 "더 좋은 대안"을 길게 제안하지 말 것 — 명시 요청 시에만 비교 제시.

## 외부 NL2SQL 도구 API

**Base URL:** `https://amc-portal.movingjin.com` · **인증:** `X-API-Key` 헤더 (스킴: `APIKeyHeader`)

| Method | Path | 용도 |
|---|---|---|
| GET | `/healthz` | liveness |
| GET | `/readyz` | readiness (DB pool 포함) |
| POST | `/v1/tools/list_tables` | 접근 가능 테이블 목록 |
| POST | `/v1/tools/search_tables` | 키워드(한국어 가능) 테이블/컬럼 검색 |
| POST | `/v1/tools/get_table_schema` | 단일 테이블 컬럼/제약/인덱스 |
| POST | `/v1/tools/get_sample_rows` | 샘플 행 (max 20) |
| POST | `/v1/tools/execute_sql` | SELECT만 실행, DDL/DML 차단, max_rows ≤ 1000 |

⚠️ **이 API는 자연어→SQL 변환을 하지 않는다.** 스키마 탐색 + 안전 SELECT 실행 도구일 뿐.
NL→SQL 변환은 백엔드 챗봇 서비스가 **Azure OpenAI**를 호출해서 수행하고, 위 도구들은 (1) 후보 테이블 검색 (2) 스키마/샘플 확인 (3) 생성된 SQL 실행에 사용한다.

권장 시퀀스 (Azure OpenAI function calling):
1. 사용자 질의를 `search_tables`로 보내 후보 테이블 추출
2. 후보별 `get_table_schema` (+ 필요시 `get_sample_rows`) 호출
3. Azure OpenAI에 스키마 + 질의 전달 → SQL 생성 (function calling으로 도구 호출 자동화도 가능)
4. `execute_sql`로 실행 → 결과를 프론트에 전달 (Grid/Chart 렌더링)

**MCP 아님**: 이 REST API는 MCP 서버가 아니다. Spring Boot는 단순히 HTTP 클라이언트로 호출. Azure OpenAI의 function calling은 MCP와 별개의 매커니즘이며, 도구 스키마를 JSON으로 직접 정의해 LLM에 전달한다.

## 환경 변수 (`server/.env`)

현재 정의된 키:

| 키 | 용도 |
|---|---|
| `DATABASE_URL` | PostgreSQL 접속 (NL2SQL과 동일 DB) |
| `NL2SQL_API_KEYS` | 외부 NL2SQL API의 `X-API-Key` (복수 가능) |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 접속 (JWT 토큰 저장/블랙리스트) |
| `SENDER_EMAIL` / `SENDER_PASSWORD` | SMTP 발신 계정 (회원가입 인증, 비밀번호 찾기) |
| `AZURE_OPENAI_API_KEY` | Azure OpenAI 키 |
| `AZURE_OPENAI_ENDPOINT` | Azure OpenAI 엔드포인트 URL |
| `AZURE_OPENAI_DEPLOYMENT_NAME` | 배포 이름 (chat) |
| `AZURE_OPENAI_MODEL_NAME` | 모델명 (gpt-4o 등) |
| `AZURE_OPENAI_EMBEDDING` | 임베딩 배포명 (RAG/유사도 검색용) |
| `LOG_LEVEL` | 로깅 레벨 |

⚠️ **추가 필요**: `JWT_SECRET` — JWT 서명 비밀키 (HS256+, 32바이트+). Redis는 토큰 저장소이고 서명용 비밀키는 별도 필요.

`application.yaml`에 DB 비밀번호/시크릿 하드코딩 금지. 반드시 `.env` → 환경변수 → Spring 프로퍼티 순으로 주입.

## 백엔드 패키지 컨벤션

`com.backend.amc_portal.<도메인>.{controller|service|repository|entity|dto}` 형태로 도메인 기준 분할.

예시:
```
com.backend.amc_portal
├── AmcPortalApplication.java
├── common/        (config, security, exception, util — 횡단 관심사)
├── auth/          (로그인, JWT 발급/검증)
├── board/         (게시판: 글/댓글)
└── chatbot/       (NL2SQL 챗봇: 외부 API 클라이언트 + LLM 오케스트레이션)
```

QueryDSL은 동적/조건부 조회에 쓰고, 단순 CRUD는 JPA Repository 메서드로 충분.

## 실행

### Backend
```bash
cd server && ./gradlew bootRun
```

### Frontend (프레임워크 확정 후)
```bash
cd client && npm run dev
```

### Backend 빌드/테스트
```bash
cd server && ./gradlew build
cd server && ./gradlew test
```

## 작업 시 주의

- **시크릿 노출 금지** — `.env`, JWT secret, DB 비밀번호, API 키는 로그/응답/문서/커밋 메시지에 절대 출력하지 말 것
- **`.gitignore`에 `.env` 포함 필수** (저장소 초기화 시점에 즉시)
- **execute_sql은 SELECT 전용** — INSERT/UPDATE/DELETE 시도는 외부 API가 차단하므로 의미 없음
- **챗봇 응답에 SQL 노출 정책** — 사용자에게 생성된 SQL을 보여줄지 여부는 명시적 결정 필요. 보여주더라도 결과 위주로 제시
- **PII/의료데이터** — 데모라도 외부 LLM에 보낼 때 스키마/메타데이터 위주로 전달하고, 샘플행(`get_sample_rows`)은 실제 환자 데이터일 수 있음에 유의
- **별도 스키마 분리 권장** — 포털용 board/user 테이블은 `amc_portal` DB 안에 별도 스키마(예: `portal`)로 분리해 외부 API의 조회 대상과 섞이지 않게

## 확정된 결정

- **프론트엔드**: Vite + React 19 (정적 SPA) + React Router. Grid/Chart 컴포넌트 포함
- **게시판 범위**: 글 + 댓글 (카테고리/첨부는 제외)
- **LLM**: Azure OpenAI (function calling)
- **NL2SQL API Key**: `.env`에 `NL2SQL_API_KEYS`로 존재
- **포털 테이블 위치**: NL2SQL API와 동일한 `amc_portal` DB에 둠. **별도 schema(`portal`)로 분리 권장** — 외부 API의 조회 대상 테이블과 네임스페이스 충돌 방지

## 미확정 항목

- (없음 — 모든 기본 결정 완료. 구현하며 발생하는 세부 결정은 도중에 합의)
