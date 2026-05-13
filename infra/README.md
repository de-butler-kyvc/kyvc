# KYvC Infra

KYvC 서비스의 Docker Compose, Dockerfile, Nginx Reverse Proxy 설정을 관리하는 인프라 디렉터리입니다.

## 1. 인프라 개요

### 인프라 역할

- dev/prod 서비스 컨테이너 실행 기준 관리
- 프론트 정적 파일 서빙과 API 프록시 설정 관리
- 서비스별 로그 마운트와 네트워크 연결 기준 관리

### 서비스 구성

- frontend
- backend
- backend_admin
- core
- core_admin
- frontend_admin
- frontend_core_admin

### dev/prod 환경

- dev compose: `compose/dev/docker-compose.yml`
- prod compose: `compose/prod/docker-compose.yml`

### 운영 기준

- 환경 변수는 compose별 `.env.*` 파일 기준
- 민감 정보는 예시 파일에 실제 값 작성 금지
- 서비스 로그는 `/opt/kyvc/logs/{env}` 하위 마운트 기준

## 2. 기술 스택

### 컨테이너

- Docker

### Reverse Proxy

- Nginx

### 배포 도구

- Docker Compose
- GitHub Container Registry 이미지

### 운영 환경

- Linux 서버
- 외부 Docker 네트워크

## 3. 디렉터리 구조

### compose

- `compose/dev`: dev 통합 실행 설정
- `compose/prod`: prod 통합 실행 설정
- `compose/postgres`: PostgreSQL 단독 실행 설정
- `compose/mysql`: MySQL 단독 실행 설정

### docker

- `docker/frontend/Dockerfile`: 프론트 정적 산출물 Nginx 이미지 빌드

### nginx

- `nginx/frontend/dev.conf`: dev 도메인과 프록시 설정
- `nginx/frontend/prod.conf`: prod 도메인과 프록시 설정

## 4. Docker 구조

### 이미지 빌드 구조

- frontend 이미지는 `infra/docker/frontend/Dockerfile` 기준 빌드
- backend, backend_admin, core, core_admin 이미지는 각 서비스 Dockerfile 기준 빌드
- compose는 GHCR 이미지 태그 `dev`, `prod` 기준 사용

### 컨테이너 실행 구조

- dev backend: `8082:8080`
- dev backend_admin: `8083:8080`
- dev core: `8092:8090`
- dev core_admin: `8093:8091`
- prod backend: `8080:8080`
- prod backend_admin: `8081:8080`
- prod core: `8090:8090`
- prod core_admin: `8091:8091`

## 5. Docker Compose 구조

### dev compose

- 파일: `compose/dev/docker-compose.yml`
- 컨테이너 접미사: `-dev`
- 네트워크: `kyvc-public`, `kyvc-internal`, `kyvc-admin-internal`, `kyvc-dev-net`

### prod compose

- 파일: `compose/prod/docker-compose.yml`
- 컨테이너 접미사: `-prod`
- 네트워크: `kyvc-public`, `kyvc-internal`, `kyvc-admin-internal`, `kyvc-prod-net`

### 네트워크

- `kyvc-public`: 외부 공개 서비스 연결
- `kyvc-internal`: backend와 core 내부 연결
- `kyvc-admin-internal`: 관리자 서비스 내부 연결
- `kyvc-dev-net`, `kyvc-prod-net`: 환경별 서비스 연결

### 볼륨

- backend 저장소: `/mnt/kyvc-dev`, `/mnt/kyvc-prod`
- 서비스 로그: `/opt/kyvc/logs/dev`, `/opt/kyvc/logs/prod`
- core issuer key: `/opt/kyvc/secrets/{env}/core/issuer-key.pem`

## 6. Nginx / Reverse Proxy 구조

### 외부 도메인

- dev 사용자: `dev-kyvc.khuoo.synology.me`
- dev 관리자: `dev-admin-kyvc.khuoo.synology.me`
- dev Core 관리자: `dev-core-admin-kyvc.khuoo.synology.me`
- prod 사용자: `kyvc.khuoo.synology.me`
- prod 관리자: `admin-kyvc.khuoo.synology.me`
- prod Core 관리자: `core-admin-kyvc.khuoo.synology.me`

### 내부 서비스 포트

- backend: `8080`
- backend_admin: `8080`
- core_admin: `8091`
- frontend Nginx: `80`

### API 프록시

- 사용자 API: `/api/` → backend
- 관리자 인증 API: `/api/admin/auth/` → backend_admin
- 관리자 본인 API: `/api/admin/me/` → backend_admin
- 관리자 업무 API: `/api/admin/backend/` → backend_admin
- Core 관리자 API: `/api/admin/core/` → core_admin

### 프론트 정적 파일 서빙

- 사용자 프론트: `/usr/share/nginx/html/frontend`
- 관리자 프론트: `/usr/share/nginx/html/frontend_admin`
- Core 관리자 프론트: `/usr/share/nginx/html/frontend_core_admin`

## 7. 환경 변수 구조

### .env.example

- dev/prod 환경별 예시는 `compose/{env}/.env.*.example` 기준
- 공통 값은 `.env.common.example` 기준
- 서비스별 값은 `.env.backend.example`, `.env.backend_admin.example`, `.env.core.example`, `.env.core_admin.example` 기준

### dev 환경 변수

- 위치: `compose/dev`
- dev DB, dev 도메인, dev 컨테이너 네트워크 기준

### prod 환경 변수

- 위치: `compose/prod`
- 운영 DB, 운영 도메인, 운영 컨테이너 네트워크 기준

## 8. 로그 구조

### 로그 마운트 경로

- dev: `/opt/kyvc/logs/dev`
- prod: `/opt/kyvc/logs/prod`

### backend 로그

- dev: `/opt/kyvc/logs/dev/backend:/app/logs`
- prod: `/opt/kyvc/logs/prod/backend:/app/logs`

### backend_admin 로그

- dev: `/opt/kyvc/logs/dev/back-admin:/app/logs`
- prod: `/opt/kyvc/logs/prod/back-admin:/app/logs`

### core 로그

- dev: `/opt/kyvc/logs/dev/core:/app/logs`
- prod: `/opt/kyvc/logs/prod/core:/app/logs`

### 로그 권한/보관 기준

- 컨테이너가 기록 가능한 권한으로 마운트
- 운영 보관 기간과 압축 정책은 서버 운영 정책 기준
