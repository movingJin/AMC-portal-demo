# AMC Portal Demo

서울아산병원(AMC) 데이터플랫폼 포털 서비스 데모. 게시판형 UI와 NL2SQL 챗봇 UI를 단일 포털로 제공합니다.

## 개요

AMC Portal Demo는 사내 데이터플랫폼 사용자를 위해 공지/문의 게시판과 자연어 기반 데이터 조회 챗봇을 결합한 포털입니다.

- **백엔드**: Spring Boot 3.5.14 + Java 21 REST API 서버
- **프론트엔드**: Vite + React 19 + TypeScript SPA
- **NL2SQL**: 외부 NL2SQL 도구 API(`amc-portal.movingjin.com`) + Azure OpenAI function calling
- **데이터베이스**: PostgreSQL
- **캐시/토큰 저장소**: Redis (JWT refresh / 블랙리스트)

## 주요 기능

- **회원가입 / 로그인**: 이메일 인증 코드 + JWT(access/refresh) 발급, Redis 기반 토큰 관리
- **비밀번호 찾기**: SMTP 메일 발송을 통한 재설정 토큰 흐름
- **게시판**: 글/댓글 작성·수정·삭제 (작성자 권한 검사)
- **NL2SQL 챗봇**: 자연어 질문 → Azure OpenAI 기반 deep-agent 파이프라인 → SQL 실행 → 표/차트 렌더링
- **결과 시각화**: AG Grid(표) + Recharts(차트)로 챗봇 응답 표현

## 프로젝트 구조

```
AMC-portal-demo/
├── server/                          # Spring Boot 3.5.14 백엔드 (Java 21, Gradle)
│   ├── build.gradle
│   ├── lombok.config
│   └── src/main/
│       ├── java/com/backend/amc_portal/
│       │   ├── AmcPortalApplication.java
│       │   │
│       │   ├── common/              # 횡단 관심사
│       │   │   ├── config/          # Security/QueryDSL/RestClient/Async/JpaAudit 설정
│       │   │   ├── security/        # JWT 필터·토큰 프로바이더, 인증 엔트리포인트
│       │   │   ├── exception/       # 전역 예외 처리
│       │   │   ├── entity/          # BaseEntity 등
│       │   │   └── dto/
│       │   │
│       │   ├── auth/                # 인증 도메인
│       │   │   ├── controller/      # /api/auth/** (signup, login, refresh, ...)
│       │   │   ├── service/         # 회원가입·이메일 인증·JWT 발급/폐기
│       │   │   ├── repository/
│       │   │   ├── entity/          # User
│       │   │   └── dto/
│       │   │
│       │   ├── board/               # 게시판 도메인
│       │   │   ├── controller/      # /api/board/**, /api/comments/**
│       │   │   ├── service/         # 글/댓글 CRUD, 권한 검사
│       │   │   ├── repository/      # JPA + QueryDSL
│       │   │   ├── entity/          # Board, Comment
│       │   │   └── dto/
│       │   │
│       │   └── chatbot/             # NL2SQL 챗봇 도메인
│       │       ├── controller/      # /api/chatbot/ask
│       │       ├── service/         # ChatbotService (오케스트레이션)
│       │       ├── agent/           # Deep-agent 서브에이전트들
│       │       │   ├── QuestionDecomposerAgent.java
│       │       │   ├── SchemaExplorerAgent.java
│       │       │   ├── SqlAuthorAgent.java
│       │       │   ├── SqlExecutorAgent.java
│       │       │   └── ResponseComposerAgent.java
│       │       ├── client/          # 외부 통신 클라이언트
│       │       │   ├── AzureOpenAiClient.java   # Azure OpenAI (function calling)
│       │       │   └── Nl2SqlClient.java        # 외부 NL2SQL 도구 API
│       │       └── dto/
│       │
│       └── resources/
│           ├── application.yaml
│           └── schema-init.sql      # 포털용 portal 스키마 DDL
│
└── client/                          # Vite + React 19 + TypeScript SPA
    ├── package.json
    ├── vite.config.ts               # dev 서버 프록시: /api/* → VITE_API_URL
    ├── tailwind.config.ts
    │
    └── src/
        ├── main.tsx                 # React 진입점 + BrowserRouter
        ├── App.tsx                  # 라우트 정의
        ├── index.css                # Tailwind
        │
        ├── pages/
        │   ├── HomePage.tsx
        │   ├── LoginPage.tsx
        │   ├── SignupPage.tsx
        │   ├── VerifyEmailPage.tsx
        │   ├── ForgotPasswordPage.tsx
        │   ├── ResetPasswordPage.tsx
        │   ├── BoardListPage.tsx
        │   ├── BoardDetailPage.tsx
        │   ├── NewBoardPage.tsx
        │   └── ChatbotPage.tsx
        │
        ├── components/
        │   ├── Navbar.tsx
        │   ├── ResultGrid.tsx       # AG Grid 결과 표
        │   └── ResultChart.tsx      # Recharts 결과 차트
        │
        └── lib/
            ├── auth.ts              # Zustand persist (localStorage)
            └── api.ts               # fetch 래퍼 + 401 자동 refresh
```

## 기술 스택

| 영역 | 선택 |
|---|---|
| Backend | Spring Boot 3.5.14, Java 21, Gradle (Groovy DSL), Lombok |
| DB Access | Spring Data JPA + QueryDSL |
| DB | PostgreSQL |
| 인증 | Spring Security + JWT (HS256) |
| Frontend | Vite + React 19 + TypeScript + React Router + Tailwind |
| Grid / Chart | AG Grid + Recharts |
| LLM | Azure OpenAI (function calling) |
| 캐시 / 세션 | Redis (refresh token / access token 블랙리스트) |
| 메일 | SMTP (회원가입 인증, 비밀번호 찾기) |

## 설치 및 실행 (로컬)

### Server

```bash
cd server

# 빌드 (QueryDSL Q-클래스는 build/generated/querydsl 아래 자동 생성)
./gradlew clean build

# DB 스키마 초기 적용 (최초 1회)
psql "$DATABASE_URL" -f src/main/resources/schema-init.sql

# 실행
./gradlew bootRun
```

서버는 `http://localhost:8080`에서 실행됩니다.

#### `server/.env` 예시

실행 전 아래 키들을 `server/.env`에 설정해야 합니다 (커밋 금지).

```env
# Database (PostgreSQL)
DATABASE_URL=postgresql://<user>:<password>@<host>:<port>/<database>

# 외부 NL2SQL 도구 API (X-API-Key 헤더 값)
NL2SQL_API_KEYS=<your-nl2sql-api-key>

# Redis (JWT refresh / 블랙리스트)
REDIS_HOST=<redis-host>
REDIS_PORT=<redis-port>
REDIS_PASSWORD=<redis-password>

# JWT 서명 비밀키 (HS256+, 32바이트 이상 권장)
# 생성 예: openssl rand -base64 48
JWT_SECRET=<random-secret>

# SMTP (회원가입 인증 코드, 비밀번호 재설정 메일 발송)
SENDER_EMAIL=<sender@example.com>
SENDER_PASSWORD=<app-password>

# Azure OpenAI (NL→SQL function calling)
AZURE_OPENAI_API_KEY=<azure-openai-key>
AZURE_OPENAI_ENDPOINT=https://<resource-name>.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT_NAME=<chat-deployment-name>
AZURE_OPENAI_MODEL_NAME=<chat-model-name>
AZURE_OPENAI_EMBEDDING=<embedding-deployment-name>

# 로깅
LOG_LEVEL=INFO
```

> `application.yaml`의 `ddl-auto: validate` 설정상 스키마 DDL이 먼저 적용되어야 부팅에 성공합니다.

### Client

```bash
cd client

# 의존성 설치
npm install

# 환경변수 (필요 시 VITE_API_URL 변경)
cp .env.example .env

# 개발 서버 실행
npm run dev
```

프론트엔드는 `http://localhost:3000`에서 실행되며, `vite.config.ts`의 dev 서버 프록시가 `/api/*` 요청을 `VITE_API_URL`(기본 `http://localhost:8080`)로 전달합니다.

## API 요약

### Auth (`/api/auth/**`) — public

- `POST /signup` `{email, password, displayName}`
- `POST /verify-email` `{email, code}` — 회원가입 시 발송된 6자리 코드
- `POST /login` `{email, password}` → `{accessToken, refreshToken, ...}`
- `POST /refresh` `{refreshToken}`
- `POST /logout` (Bearer) — access 토큰 블랙리스트 + refresh 폐기
- `POST /forgot-password` `{email}`
- `POST /reset-password` `{token, newPassword}`
- `GET /me` (Bearer)

### Board (`/api/board/**`)

- `GET /api/board?keyword=&page=&size=` — public
- `GET /api/board/{id}` — public
- `POST /api/board` (Bearer)
- `PUT /api/board/{id}` (Bearer, owner only)
- `DELETE /api/board/{id}` (Bearer, owner only)

### Comments

- `GET /api/board/{boardId}/comments`
- `POST /api/board/{boardId}/comments` (Bearer)
- `PUT /api/comments/{id}` (Bearer, owner only)
- `DELETE /api/comments/{id}` (Bearer, owner only)

### Chatbot

- `POST /api/chatbot/ask` (Bearer) `{question}` → `{answer, sql, columns, rows, trace}`

## 외부 NL2SQL 도구 API

**Base URL:** `https://amc-portal.movingjin.com` · **인증:** `X-API-Key` 헤더

| Method | Path | 용도 |
|---|---|---|
| GET | `/healthz` | liveness |
| GET | `/readyz` | readiness (DB pool 포함) |
| POST | `/v1/tools/list_tables` | 접근 가능 테이블 목록 |
| POST | `/v1/tools/search_tables` | 키워드(한국어 가능) 테이블/컬럼 검색 |
| POST | `/v1/tools/get_table_schema` | 단일 테이블 컬럼/제약/인덱스 |
| POST | `/v1/tools/get_sample_rows` | 샘플 행 (max 20) |
| POST | `/v1/tools/execute_sql` | SELECT만 실행, DDL/DML 차단, max_rows ≤ 1000 |

이 API는 자연어→SQL 변환을 하지 않습니다. NL→SQL 변환은 백엔드 챗봇 서비스가 Azure OpenAI를 호출해 수행하고, 위 도구들은 (1) 후보 테이블 검색 (2) 스키마/샘플 확인 (3) 생성된 SQL 실행에 사용합니다.

## 빌드 / 배포

### Backend

```bash
cd server && ./gradlew clean build
# 산출물: build/libs/*.jar
```

### Frontend

```bash
cd client && npm run build
# 산출물: dist/ — nginx, Caddy, S3+CloudFront, GitHub Pages 등 정적 호스팅 가능
```

프로덕션에서는 (a) 백엔드 CORS 허용 도메인에 정적 호스팅 URL을 추가하거나, (b) 동일 도메인에서 nginx로 `/api/*`만 백엔드로 프록시하는 구성을 권장합니다.

## 주의 사항

- `server/.env`는 커밋 금지 (`.gitignore`에 포함)
- `execute_sql`은 SELECT 전용이며, DML/DDL은 외부 API가 차단
- 챗봇 응답에 SQL 노출 여부는 정책에 따라 결정 (현재는 응답에 `sql` 필드 포함)
- 포털용 board/user 테이블은 외부 NL2SQL API 조회 대상과 분리하기 위해 `portal` 스키마에 둠
