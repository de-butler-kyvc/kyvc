# KYvC

KYvC는 법인 KYC 심사, Verifiable Credential 발급, Verifiable Presentation 검증, XRPL DID/자격증명 상태 관리를 제공하는 모노레포 프로젝트입니다. 사용자 서비스, 운영자 서비스, Core 자격증명 서비스, 인프라 설정을 한 저장소에서 관리합니다.

## 서비스 구성

| 디렉터리 | 역할 | 주요 기술 |
| --- | --- | --- |
| `frontend` | 법인 고객, 금융기관, 웹/모바일 지갑 사용자 화면 | Next.js 16, React 19, TypeScript |
| `frontend_admin` | KYvC 운영 관리자 백오피스 | Next.js 16, React 19, TypeScript |
| `frontend_core_admin` | Core 운영 관리자 화면 | Next.js 16, React 19, TypeScript |
| `frontend_core_admin_api` | Core Admin API 운영 콘솔 | Next.js 16, React 19, TypeScript |
| `backend` | 사용자 서비스 API, KYC 신청/문서/인증/VC 연동 | Spring Boot 3.5, Java 21, PostgreSQL |
| `backend_admin` | 운영 관리자 API, KYC/사용자/VC/VP 관리 | Spring Boot 3.5, Java 21, PostgreSQL |
| `core` | DID, VC/VP, SD-JWT, XRPL, AI 문서 평가 Core API | FastAPI, Python 3.12, MySQL |
| `core_admin` | Core 운영용 Admin API 어댑터 | FastAPI, Python 3.12 |
| `infra` | Dockerfile, docker compose, Nginx 기반 배포 구성 | Docker, Compose, Nginx |

## 주요 기능

- 법인 고객 회원가입, 로그인, KYC 신청, 서류 제출, 진행 상태 조회
- KYC 심사 결과 기반 VC 발급 및 폐기
- 금융기관 VP 요청, 제출, 검증 흐름
- 모바일 WebView 지갑 기반 VC 목록, QR, VP 제출, XRP 송수신
- XRPL DID 등록 및 Credential 상태 원장 연동
- SD-JWT 기반 자격증명 발급/검증
- OCR/LLM 기반 KYC 문서 AI 평가
- 운영자용 KYC, 사용자, VC/VP, Issuer/Verifier, 정책, 감사로그 관리
- Core 운영자용 provider 선택, Core 상태 조회, 변경 이력 관리

## 저장소 구조

```text
kyvc/
├── backend/                  # 사용자 서비스 Spring Boot API
├── backend_admin/            # 관리자 Spring Boot API
├── core/                     # VC/VP, DID, XRPL, AI 평가 Core API
├── core_admin/               # Core 운영 Admin API
├── frontend/                 # 사용자/법인/금융기관/모바일 지갑 프론트엔드
├── frontend_admin/           # 운영 관리자 프론트엔드
├── frontend_core_admin/      # Core 관리자 프론트엔드
├── frontend_core_admin_api/  # Core Admin API 콘솔
└── infra/                    # Docker, compose, Nginx 배포 구성
```

## 요구 사항

- Java 21
- Python 3.12
- Node.js 20 이상
- Docker, Docker Compose
- PostgreSQL 16
- MySQL 8.4

Next.js 16 기반 프론트엔드는 Node.js 20 이상에서 실행해야 합니다.

## 빠른 시작

### 1. 데이터베이스 실행

개발용 PostgreSQL과 MySQL compose 파일은 `infra/compose` 아래에 있습니다.

```bash
docker compose -f infra/compose/postgres/docker-compose.yml up -d kyvc-postgres-back-dev
docker compose -f infra/compose/mysql/docker-compose.yml up -d kyvc-mysql-core-dev
```

compose는 외부 Docker network를 사용합니다. 네트워크가 없다면 먼저 생성합니다.

```bash
docker network create kyvc-dev-net
docker network create kyvc-prod-net
```

개발 DB 기본 포트:

| 서비스 | 로컬 포트 | 컨테이너 포트 |
| --- | --- | --- |
| PostgreSQL dev | `5433` | `5432` |
| MySQL dev | `3307` | `3306` |

### 2. 환경 변수 준비

각 서비스의 예시 파일을 복사해 로컬 환경에 맞게 수정합니다.

```bash
cp backend/.env.example backend/.env
cp backend_admin/.env.example backend_admin/.env
cp core/.env.example core/.env
cp core_admin/.env.example core_admin/.env
```

민감 정보, DB 비밀번호, JWT secret, 메일 계정, OpenAI/Azure/Naver OCR 키, XRPL issuer seed 등은 실제 환경 값으로 설정해야 합니다.

### 3. Core 실행

```bash
cd core
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090 --reload
```

문서:

```text
http://localhost:8090/docs
```

### 4. Core Admin API 실행

```bash
cd core_admin
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8091 --reload
```

### 5. Backend 실행

```bash
cd backend
./gradlew bootRun
```

기본 포트:

```text
http://localhost:8080
```

### 6. Backend Admin 실행

```bash
cd backend_admin
./gradlew bootRun
```

기본 포트:

```text
http://localhost:8080
```

`backend`와 동시에 실행할 경우 `SERVER_PORT` 또는 Spring 설정으로 포트를 분리하세요.

### 7. 프론트엔드 실행

각 프론트엔드 앱은 독립 Next.js 프로젝트입니다.

```bash
cd frontend
npm install
npm run dev
```

관리자 프론트엔드:

```bash
cd frontend_admin
npm install
npm run dev
```

Core 관리자 프론트엔드:

```bash
cd frontend_core_admin
npm install
npm run dev
```

Core Admin API 콘솔:

```bash
cd frontend_core_admin_api
npm install
npm run dev
```

여러 Next.js 앱을 동시에 실행할 때는 포트를 분리합니다.

```bash
npm run dev -- -p 3001
```

## 주요 로컬 포트

| 서비스 | 기본 포트 |
| --- | --- |
| `frontend` | `3000` |
| `frontend_admin` | `3000` |
| `frontend_core_admin` | `3000` |
| `frontend_core_admin_api` | `3000` |
| `backend` | `8080` |
| `backend_admin` | `8080` |
| `core` | `8090` |
| `core_admin` | `8091` |
| PostgreSQL dev | `5433` |
| MySQL dev | `3307` |

## API와 서비스 연동

일반적인 호출 흐름은 다음과 같습니다.

```text
frontend / frontend_admin
        ↓
backend / backend_admin
        ↓
core / core_admin
        ↓
PostgreSQL, MySQL, XRPL, OCR/LLM provider
```

`backend`는 사용자 인증, KYC 신청, 문서 저장, Core 호출을 담당합니다. `core`는 DID, VC/VP, SD-JWT, XRPL 상태, AI 평가를 담당합니다. `backend_admin`은 운영자 API를 제공하며, `core_admin`은 Core 내부 운영 API를 안전하게 호출하는 어댑터 역할을 합니다.

## 배포 구성

배포 compose 파일은 `infra/compose` 아래에 있습니다.

| 파일 | 설명 |
| --- | --- |
| `infra/compose/dev/docker-compose.yml` | dev 애플리케이션 서비스 |
| `infra/compose/prod/docker-compose.yml` | prod 애플리케이션 서비스 |
| `infra/compose/postgres/docker-compose.yml` | PostgreSQL dev/prod |
| `infra/compose/mysql/docker-compose.yml` | MySQL dev/prod |

dev 배포 포트:

| 서비스 | 포트 |
| --- | --- |
| frontend | `3001:80` |
| backend | `8082:8080` |
| core | `8092:8090` |
| backend_admin | `8083:8080` |
| core_admin | `8093:8091` |

prod 배포 포트:

| 서비스 | 포트 |
| --- | --- |
| frontend | `3000:80` |
| backend | `8080:8080` |
| core | `8090:8090` |
| backend_admin | `8081:8080` |
| core_admin | `8091:8091` |

## 검증 명령

Spring Boot:

```bash
cd backend
./gradlew test
```

```bash
cd backend_admin
./gradlew test
```

Core:

```bash
cd core
pytest
```

Core Admin:

```bash
cd core_admin
pytest
```

Frontend:

```bash
cd frontend
npm run build
```

```bash
cd frontend_admin
npm run build
```

```bash
cd frontend_core_admin
npm run build
```

```bash
cd frontend_core_admin_api
npm run build
```

TypeScript 타입 체크만 확인하려면 각 프론트엔드 디렉터리에서 다음 명령을 사용할 수 있습니다.

```bash
node ./node_modules/typescript/bin/tsc --noEmit
```

## Git 운영 전략

브랜치는 `main`, `develop`, `feature/*`만 사용합니다.

- `main`: production 자동 배포 기준
- `develop`: dev 자동 배포 기준
- `feature/*`: 기능 개발용, 배포하지 않음

작업 흐름:

```text
feature/* -> develop -> main
```

`main`, `develop` 브랜치에는 직접 커밋하거나 푸시하지 않고 PR 병합만 허용합니다.

커밋 메시지 형식:

```text
type(scope): 작업 내용
```

사용 가능한 `type`:

- `feat`
- `fix`
- `refactor`
- `test`
- `docs`
- `chore`

사용 가능한 `scope`:

- `back`
- `back-admin`
- `front`
- `front-admin`
- `core`
- `core-admin`
- `infra`
- `-`

예시:

```text
feat(back): 검증 요청 API 추가
fix(front): 모바일 VP 제출 오류 처리 수정
docs(-): 프로젝트 README 작성
chore(infra): dev compose 포트 정리
```

## 참고 문서

- `core/README.md`: Core API 상세 설명
- `core_admin/README.md`: Core Admin API 설명
- `frontend_admin/README.md`: 운영 관리자 프론트엔드 설명
- `frontend_core_admin_api/README.md`: Core Admin API 콘솔 설명
- `frontend/AI_HANDOFF.md`: 모바일 지갑 및 프론트엔드 연동 참고
