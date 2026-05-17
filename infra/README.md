# KYvC Infra

## 1. 인프라 개요

### 인프라 역할

KYvC 서비스의 Docker Compose, Synology DSM Reverse Proxy, Frontend 통합 Nginx, dev/prod 배포, 로그/볼륨, 환경변수, 운영 구조를 관리한다.

### 서비스 구성

| 구분          | 서비스                                                                                             |
| ------------- | -------------------------------------------------------------------------------------------------- |
| Frontend      | `kyvc-frontend-dev`, `kyvc-frontend-prod`                                                          |
| Backend       | `kyvc-backend-dev`, `kyvc-backend-prod`                                                            |
| Backend Admin | `kyvc-back-admin-dev`, `kyvc-back-admin-prod`                                                      |
| Core          | `kyvc-core-dev`, `kyvc-core-prod`                                                                  |
| Core Admin    | `kyvc-core-admin-dev`, `kyvc-core-admin-prod`                                                      |
| Database      | `kyvc-postgres-back-dev`, `kyvc-postgres-back-prod`, `kyvc-mysql-core-dev`, `kyvc-mysql-core-prod` |
| Reverse Proxy | Synology DSM Reverse Proxy / Frontend 통합 Nginx                                                   |

### dev/prod 환경

- `develop` 브랜치는 dev 환경 기준이다.
- `main` 브랜치는 prod 환경 기준이다.
- dev compose 파일은 `infra/compose/dev/docker-compose.yml`이다.
- prod compose 파일은 `infra/compose/prod/docker-compose.yml`이다.

### 운영 기준

- `main`, `develop` 직접 push는 금지한다.
- 배포는 PR 병합 기반으로 진행한다.
- 민감정보는 `.env.example`에 실제 값으로 작성하지 않는다.
- 서비스 로그와 문서 저장소는 host volume 기준으로 관리한다.

## 2. 기술 스택

### 컨테이너

- Docker

### Reverse Proxy

- Synology DSM Reverse Proxy
- Frontend 통합 Nginx

### 배포 도구

- Docker Compose
- GitHub Actions
- GitHub Container Registry 이미지

### 운영 환경

- 운영 서버 환경 기준
- 컨테이너 timezone은 `Asia/Seoul` 기준

## 3. 디렉터리 구조

### compose

| 경로                     | 역할                                   |
| ------------------------ | -------------------------------------- |
| `infra/compose/dev`      | dev 통합 compose와 dev 환경변수 예시   |
| `infra/compose/prod`     | prod 통합 compose와 prod 환경변수 예시 |
| `infra/compose/postgres` | backend PostgreSQL 단독 compose        |
| `infra/compose/mysql`    | core MySQL 단독 compose                |

### docker

| 경로                               | 역할                                                                              |
| ---------------------------------- | --------------------------------------------------------------------------------- |
| `infra/docker/frontend/Dockerfile` | `frontend`, `frontend_admin`, `frontend_core_admin` 정적 빌드와 Nginx 이미지 생성 |

### nginx

`infra/nginx/frontend/*.conf`는 프론트 통합 Nginx 전용 reverse proxy 설정이다.

| 경로                             | 역할                                              |
| -------------------------------- | ------------------------------------------------- |
| `infra/nginx/frontend/dev.conf`  | dev 프론트 통합 Nginx 도메인과 API reverse proxy  |
| `infra/nginx/frontend/prod.conf` | 프론트 통합 Nginx API reverse proxy |

## 4. Docker 구조

### 이미지 빌드 구조

- frontend 통합 이미지는 `infra/docker/frontend/Dockerfile` 기준으로 빌드한다.
- frontend 통합 이미지는 `frontend`, `frontend_admin`, `frontend_core_admin`을 각각 정적 빌드한 뒤 Nginx 정적 파일 경로에 복사한다.
- backend 이미지는 `backend/Dockerfile` 기준으로 Java 21 JDK 빌드 후 Java 21 JRE 런타임으로 실행한다.
- backend_admin 이미지는 `backend_admin/Dockerfile` 기준으로 Java 21 JDK 빌드 후 Java 21 JRE 런타임으로 실행한다.
- compose는 GHCR 이미지 태그 `dev`, `prod` 기준을 사용한다.

### 컨테이너 실행 구조

| 환경 | 컨테이너      | host:container |
| ---- | ------------- | -------------- |
| dev  | frontend      | `3001:80`      |
| dev  | backend       | `8082:8080`    |
| dev  | backend_admin | `8083:8080`    |
| dev  | core          | `8092:8090`    |
| dev  | core_admin    | `8093:8091`    |
| prod | frontend      | `3000:80`      |
| prod | backend       | `8080:8080`    |
| prod | backend_admin | `8081:8080`    |
| prod | core          | `8090:8090`    |
| prod | core_admin    | `8091:8091`    |

## 5. Docker Compose 구조

### dev compose

- 파일: `infra/compose/dev/docker-compose.yml`
- 주요 서비스: `kyvc-frontend-dev`, `kyvc-backend-dev`, `kyvc-back-admin-dev`, `kyvc-core-dev`, `kyvc-core-admin-dev`
- env 파일: `.env.common`, `.env.backend`, `.env.backend_admin`, `.env.core`, `.env.core_admin`

### prod compose

- 파일: `infra/compose/prod/docker-compose.yml`
- 주요 서비스: `kyvc-frontend-prod`, `kyvc-backend-prod`, `kyvc-back-admin-prod`, `kyvc-core-prod`, `kyvc-core-admin-prod`
- env 파일: `.env.common`, `.env.backend`, `.env.backend_admin`, `.env.core`, `.env.core_admin`

### 네트워크

| 네트워크              | 역할                                          |
| --------------------- | --------------------------------------------- |
| `kyvc-public`         | 외부 공개 프론트와 backend 연결               |
| `kyvc-internal`       | backend와 core 내부 연결                      |
| `kyvc-admin-internal` | 관리자 프론트와 backend_admin/core_admin 연결 |
| `kyvc-dev-net`        | dev 환경 서비스 연결                          |
| `kyvc-prod-net`       | prod 환경 서비스 연결                         |

## 6. Nginx / Reverse Proxy 구조

### 외부 도메인

| 도메인                                  | 환경           | 대상 서비스                                                             |
| --------------------------------------- | -------------- | ----------------------------------------------------------------------- |
| `dev-kyvc.khuoo.synology.me`            | dev            | Synology DSM Reverse Proxy / Frontend 통합 Nginx / 사용자 프론트        |
| `dev-admin-kyvc.khuoo.synology.me`      | dev admin      | Synology DSM Reverse Proxy / Frontend 통합 Nginx / 백엔드 어드민 프론트 |
| `dev-core-admin-kyvc.khuoo.synology.me` | dev core admin | Synology DSM Reverse Proxy / Frontend 통합 Nginx / 코어 어드민 프론트   |
| `dev-api-kyvc.khuoo.synology.me`        | dev            | Synology DSM Reverse Proxy / Backend API                                |
| `dev-core-kyvc.khuoo.synology.me`       | dev            | Synology DSM Reverse Proxy / Core API                                   |
| `dev-admin-api-kyvc.khuoo.synology.me`  | dev admin      | Synology DSM Reverse Proxy / Backend Admin API                          |
| `dev-admin-core-kyvc.khuoo.synology.me` | dev admin      | Synology DSM Reverse Proxy / Core Admin API                             |

### 내부 서비스 포트

| 서비스        | dev 내부 대상                     | prod 내부 대상                     |
| ------------- | --------------------------------- | ---------------------------------- |
| backend       | `http://kyvc-backend-dev:8080`    | `http://kyvc-backend-prod:8080`    |
| backend_admin | `http://kyvc-back-admin-dev:8080` | `http://kyvc-back-admin-prod:8080` |
| core_admin    | `http://kyvc-core-admin-dev:8091` | `http://kyvc-core-admin-prod:8091` |

### API 프록시

| 도메인             | Prefix                | 대상          |
| ------------------ | --------------------- | ------------- |
| 사용자 프론트      | `/api/`               | backend       |
| 관리자 프론트      | `/api/admin/auth/`    | backend_admin |
| 관리자 프론트      | `/api/admin/me/`      | backend_admin |
| 관리자 프론트      | `/api/admin/backend/` | backend_admin |
| Core 관리자 프론트 | `/api/admin/core/`    | core_admin    |

### 프론트 정적 파일 서빙

| 서비스              | Nginx root                                  |
| ------------------- | ------------------------------------------- |
| frontend            | `/usr/share/nginx/html/frontend`            |
| frontend_admin      | `/usr/share/nginx/html/frontend_admin`      |
| frontend_core_admin | `/usr/share/nginx/html/frontend_core_admin` |

## 7. 주요 환경 변수

- `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`: dev/prod DB 비밀번호를 실제 값으로 변경
- `KYVC_JWT_SECRET`: backend, backend_admin 운영 JWT 비밀키를 실제 값으로 변경
- `KYVC_CORS_ALLOWED_ORIGINS`: 실제 frontend, frontend_admin 도메인으로 변경
- `KYVC_CORE_BASE_URL`, `CORE_BASE_URL`, `CORE_ADMIN_BASE_URL`: 배포 환경의 Core/Core Admin 서비스 URL로 변경
- `FRONTEND_ADMIN_API_BASE_URL`: 관리자 프론트 빌드 시 사용할 backend_admin API URL로 변경
- `APP_STORAGE_PATH`, `LOG_PATH`, `SERVICE_LOG_PATH`: 서버 볼륨 마운트 경로로 변경
- `KYVC_DOCUMENT_STORAGE_PATH`: backend 문서 저장소 컨테이너 경로로 변경

## 8. 로그 구조

### 로그 마운트 경로

| 구분 | 컨테이너 경로 | Host 경로 노출 정책 | 권한 기준 |
| --- | --- | --- | --- |
| backend | `/app/logs` | 운영 환경변수와 compose 실제 값 기준, README 비공개 | 쓰기 가능 |
| backend_admin | `/app/logs` | 운영 환경변수와 compose 실제 값 기준, README 비공개 | 쓰기 가능 |
| core | `/app/logs` | 운영 환경변수와 compose 실제 값 기준, README 비공개 | 쓰기 가능 |
| core_admin | `/app/logs` | 운영 환경변수와 compose 실제 값 기준, README 비공개 | 쓰기 가능 |
| service logs 조회 | `/app/service-logs` | 운영 환경변수와 compose 실제 값 기준, README 비공개 | 읽기 전용 |

### 로그 권한/보관 기준

| 항목 | 운영 기준 |
| --- | --- |
| 서비스 로그 쓰기 | 컨테이너가 기록 가능한 권한으로 `/app/logs` 마운트 |
| 로그 조회 마운트 | service logs 조회용 경로는 `/app/service-logs` 읽기 전용 마운트 |
| Host 경로 관리 | 운영 서버 절대경로는 README에 노출하지 않고 환경변수와 compose 실제 값으로 관리 |
| 보관 기간 | dev/prod 로그 30일 보관 기준 |
| 로테이션 | 일 단위 또는 크기 기준 로테이션 |
| 로그 금지 데이터 | JWT, Refresh Token, VC/VP 원문, private key, 문서 원문, Core raw payload |
