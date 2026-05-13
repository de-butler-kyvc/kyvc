# KYvC Backend Admin Service

KYvC 관리자 서비스 API 서버입니다. 운영 관리자가 KYC 심사, VC/VP 운영, 사용자와 법인 관리, 정책, 감사로그, 리포트를 처리하는 백오피스 API를 제공합니다.

## 1. 서비스 개요

### 담당 사용자

- KYvC 운영 관리자
- KYC 심사 담당자
- Issuer / Verifier 운영 담당자
- 보안과 감사 담당자

### 호출 주체

- `frontend_admin`

### 서비스 역할

- 관리자 인증과 세션 관리
- KYC 신청 조회와 수동 심사
- VC 발급, 재발급, 폐기 운영
- 사용자, 법인, Issuer, Verifier 관리
- 공통코드, 정책, 알림, 감사로그, 리포트 관리

### 서비스 도메인

- 관리자 인증
- KYC 심사
- 문서 삭제 요청
- Credential
- VP 검증
- Issuer 정책
- Verifier 관리
- 공통코드
- 감사로그
- 운영 리포트

### Swagger 주소

- OpenAPI: `https://dev-admin-api-kyvc.khuoo.synology.me/swagger-ui/index.html`

## 2. 기술 스택

### 언어

- Java 21

### 프레임워크

- Spring Boot 3.5.13
- Spring MVC
- Spring Security
- Spring Data JPA

### 빌드 도구

- Gradle Wrapper

### 데이터베이스

- PostgreSQL
- Flyway 설정 포함

### 주요 라이브러리

- springdoc-openapi
- jjwt
- WebFlux WebClient
- Bean Validation
- Lombok
- Spring Mail
- OpenPDF
- spring-dotenv

## 3. API 구조

### API 그룹

- `/api/admin/auth`: 관리자 로그인, 로그아웃, 토큰 갱신, MFA
- `/api/admin/me`: 관리자 본인 정보
- `/api/admin/backend/dashboard`: 관리자 대시보드
- `/api/admin/backend/kyc`: KYC 신청, 문서, 심사
- `/api/admin/backend/credentials`: VC 발급과 상태 관리
- `/api/admin/backend/vp-verifications`: VP 검증 이력
- `/api/admin/backend/users`: 사용자와 법인 관리
- `/api/admin/backend/verifiers`: Verifier 관리
- `/api/admin/backend/issuer-*`: Issuer 설정과 정책
- `/api/admin/backend/common-*`: 공통코드 관리
- `/api/admin/backend/audit-logs`: 감사로그
- `/api/admin/backend/reports`: 운영 리포트

### 주요 API

- 관리자 로그인과 세션 확인
- KYC 신청 목록과 상세 조회
- KYC 수동 승인, 반려, 보완 요청
- VC 발급, 재발급, 폐기 처리
- 사용자 상태 변경과 법인 상세 조회
- Verifier 승인, 중지, 키 관리
- 공통코드 생성, 수정, 활성화, 비활성화
- 운영 리포트 조회와 PDF 내보내기

### 요청/응답 규칙

- 모든 API 응답은 `CommonResponse` 기준
- Controller 응답 생성은 `CommonResponseFactory` 사용
- 비즈니스 예외는 `ApiException`과 `ErrorCode` 사용
- 예외 응답은 `GlobalExceptionHandler` 기준 처리

## 4. 패키지 구조

### global

- 공통 응답, 예외, JWT, 보안, 로그, 필터, 메일, 공통코드

### domain

- `admin`, `auth`, `dashboard`, `kyc`, `document`, `review`, `credential`, `vp`, `issuer`, `verifier`, `corporate`, `audit`, `security`, `notification`, `report`, `core`

### resources

- `application.yml`
- `application-local.yml`
- `application-dev.yml`
- `application-prod.yml`
- `logback-spring.xml`

## 5. DB 구조

### DB 조성 기준

- DBMS는 PostgreSQL 16 기준입니다.
- 기본 DB명은 `kyvc_back`입니다.
- 로컬 기본 포트는 `5433`입니다.
- backend_admin은 backend와 동일한 DB 스키마를 사용합니다.
- 현재 backend_admin에는 별도 `db/migration` 디렉터리가 없습니다.
- 스키마 생성과 seed 데이터 반영은 backend Flyway migration 기준입니다.
- backend_admin의 `.env.example`은 `FLYWAY_ENABLED=false` 기준입니다.

### 로컬 DB 준비 순서

repo root 기준 PostgreSQL compose를 실행합니다.

```bash
cd infra/compose/postgres
cp .env.example .env
docker compose --env-file .env up -d kyvc-postgres-back-dev
```

Windows PowerShell에서는 `cp` 대신 다음 명령을 사용할 수 있습니다.

```powershell
Copy-Item .env.example .env
```

그 다음 backend를 먼저 실행해 스키마를 생성합니다.

```bash
cd backend
./gradlew bootRun
```

backend migration 적용 후 backend_admin을 실행합니다.

```bash
cd backend_admin
./gradlew bootRun
```

### 로컬 DB 환경 변수

```env
SPRING_PROFILES_ACTIVE=local
FLYWAY_ENABLED=false
DB_HOST=localhost
DB_PORT=5433
DB_NAME=kyvc_back
DB_USER=kyvc_back_dev
DB_PASSWORD=CHANGE_ME_DEV_POSTGRES_PASSWORD
```

`DB_PASSWORD`는 PostgreSQL compose의 `POSTGRES_BACK_DEV_PASSWORD`와 동일해야 합니다.

### 주요 사용 테이블

- 관리자 인증: `admin_users`, `admin_roles`, `admin_user_roles`
- 사용자/법인 관리: `users`, `corporates`, `corporate_documents`, `corporate_representatives`, `corporate_agents`
- KYC 심사: `kyc_applications`, `kyc_documents`, `kyc_supplements`, `kyc_review_histories`, `document_delete_requests`
- Credential 운영: `credentials`, `credential_requests`, `credential_status_histories`, `credential_offers`
- Verifier 운영: `verifiers`, `verifier_api_keys`, `verifier_callbacks`, `verifier_logs`, `vp_verifications`
- 정책/공통코드: `common_code_groups`, `common_codes`, `document_requirements`, `ai_review_policies`, `issuer_configs`, `issuer_policies`
- 운영 로그: `audit_logs`, `notifications`, `notification_templates`

### Migration 책임

- backend_admin은 현재 스키마를 직접 생성하지 않습니다.
- 신규 테이블이나 컬럼이 필요하면 backend migration 소유 범위를 먼저 확인합니다.
- 관리자 전용 migration을 추가해야 할 경우 backend_admin에 migration 디렉터리와 Flyway 정책을 별도 합의 후 추가합니다.
- 운영 환경에서 `FLYWAY_ENABLED=true`로 변경하기 전에 migration 파일 존재 여부와 적용 책임을 확인합니다.

### DB 초기화 확인

- `flyway_schema_history`에 backend migration 적용 이력이 존재해야 합니다.
- `admin_users`와 `admin_roles` seed 데이터가 있어야 관리자 로그인이 가능합니다.
- `common_code_groups`, `common_codes` seed 데이터가 있어야 문서유형, 법인유형, 심사 사유 조회가 가능합니다.
- backend_admin 실행 전 backend migration이 완료되어야 Repository 조회 오류를 방지할 수 있습니다.

### Repository 구조

- 단순 저장/조회: `XxxRepository` + `XxxRepositoryImpl`
- 목록/검색/통계: `XxxQueryRepository` + `XxxQueryRepositoryImpl`
- Service에서 JPA Repository 직접 사용 금지

## 6. 인증/인가 구조

### JWT

- `JwtTokenProvider`에서 발급과 검증 처리
- Refresh Token 저장 시 `TokenHashUtil.sha256(...)` 사용

### Cookie

- Access Token과 Refresh Token은 HttpOnly Cookie 기준
- Cookie 처리는 `TokenCookieUtil` 사용

### Role

- 관리자 권한은 Role 기반으로 처리
- 인증 사용자 정보는 `CustomUserDetails` 기준

### 내부 API 인증

- backend 연동은 `KYVC_BACKEND_BASE_URL` 기준
- 내부 호출 보안 값은 `KYVC_BACKEND_INTERNAL_API_KEY` 기준

## 7. 외부 연동 구조

### Core 연동

- backend_admin에서 Core 직접 호출 금지
- 필요한 Core 관련 처리는 backend 또는 승인된 내부 API 경유

### DB 연동

- PostgreSQL JDBC
- JPA Repository 구현체 기준 접근

### 파일 저장소

- 관리자 문서 미리보기와 로그 조회는 설정된 저장소와 서비스 로그 경로 사용

### 외부 API

- backend 내부 API
- SMTP 메일 서버

## 8. 환경 변수 구조

### 로컬 환경

- 기준 파일: `.env.example`
- 주요 값: `SPRING_PROFILES_ACTIVE`, `DB_*`, `KYVC_JWT_*`, `KYVC_CORS_*`, `KYVC_BACKEND_*`, `KYVC_MAIL_*`

### dev 환경

- `SPRING_PROFILES_ACTIVE=dev`
- dev DB, dev 관리자 프론트, dev backend 기준 설정

### prod 환경

- `SPRING_PROFILES_ACTIVE=prod`
- 운영 DB, 운영 관리자 프론트, 운영 backend 기준 설정

## 9. 실행 구조

### 로컬 실행

```bash
cd backend_admin
./gradlew bootRun
```

### Docker 실행

```bash
cd backend_admin
docker build -t kyvc-backend-admin .
docker run --env-file .env -p 8081:8080 kyvc-backend-admin
```

### 빌드

```bash
cd backend_admin
./gradlew clean build
```

## 10. 개발 규칙

### 디렉터리 작업 범위

- 관리자 API는 `backend_admin` 내부에서만 작성
- 사용자 API는 `backend`에서 작성
- Core 직접 호출 구조는 추가하지 않음

### 응답 포맷

- Controller에서 `new CommonResponse(...)` 직접 생성 금지
- `CommonResponseFactory` 사용

### 예외 처리

- `RuntimeException` 직접 throw 금지
- `ApiException`과 `ErrorCode` 사용
- 예외 응답은 `GlobalExceptionHandler` 기준 처리
