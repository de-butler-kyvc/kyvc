# KYvC Backend Service

KYvC 사용자 서비스 API 서버입니다. 법인 사용자 가입, KYC 신청, 문서 제출, VC 발급 요청, VP 검증 연동, 모바일 지갑 연동을 담당합니다.

## 1. 서비스 개요

### 담당 사용자

- 법인 사용자
- 금융기관 사용자
- 모바일 지갑 사용자
- Verifier 연동 사용자

### 호출 주체

- `frontend`
- 모바일 지갑 클라이언트
- 금융기관 연동 화면
- 내부 서비스

### 서비스 역할

- 사용자 인증과 세션 관리
- 법인 KYC 신청과 문서 관리
- Core 기반 VC 발급과 VP 검증 중계
- 공통코드, 알림, 감사로그 처리

### 서비스 도메인

- 인증
- KYC
- 문서
- 법인
- Credential
- VP
- Verifier
- 모바일 지갑

### Swagger 주소

- OpenAPI: `https://dev-api-kyvc.khuoo.synology.me/swagger-ui/index.html#/`

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
- Flyway Migration

### 주요 라이브러리

- springdoc-openapi
- jjwt
- WebFlux WebClient
- Bean Validation
- Lombok
- Spring Mail

## 3. API 구조

### API 그룹

- `/api/auth`: 로그인, 로그아웃, 토큰 갱신, MFA, 비밀번호 재설정
- `/api/common`: 세션, 공통코드, 약관, 알림, DID 기관 조회
- `/api/user`: 사용자 대시보드, 법인 정보, 문서, Credential, VP 이력
- `/api/corporate`: 법인 KYC 신청과 문서 제출
- `/api/mobile`: 모바일 인증, 지갑, VP 제출
- `/api/finance`: 금융기관 KYC와 VP 검증 연동
- `/api/verifier`: Verifier 앱과 VP 요청 연동
- `/api/internal`: 내부 헬스체크, Core 콜백, 감사로그, 알림

### 주요 API

- 회원가입과 로그인
- 법인 KYC 신청 생성과 제출
- KYC 문서 업로드와 미리보기
- Credential 발급, 재발급, 폐기 요청
- 모바일 지갑 Credential 수락
- VP 요청 생성과 제출
- Core 헬스체크와 내부 요청 기록

### 요청/응답 규칙

- 모든 API 응답은 `CommonResponse` 기준
- Controller 응답 생성은 `CommonResponseFactory` 사용
- 비즈니스 예외는 `ApiException`과 `ErrorCode` 사용
- 예외 응답은 `GlobalExceptionHandler` 기준 처리

## 4. 패키지 구조

### global

- 공통 응답, 예외, 보안, JWT, 로그, 필터, 설정, 공통 유틸

### domain

- `auth`, `kyc`, `document`, `corporate`, `credential`, `vp`, `verifier`, `mobile`, `finance`, `core`, `commoncode`, `notification`, `audit`

### resources

- `application.yml`
- `application-local.yml`
- `application-dev.yml`
- `application-prod.yml`
- `db/migration`
- `logback-spring.xml`

## 5. DB 구조

### DB 조성 기준

- DBMS는 PostgreSQL 16 기준입니다.
- 로컬 기본 DB명은 `kyvc_back`입니다.
- 로컬 기본 포트는 `5433`입니다.
- 로컬 기본 사용자는 `kyvc_back_dev`입니다.
- 접속 정보는 `.env.example`의 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`를 기준으로 맞춥니다.
- 스키마 생성과 초기 데이터 반영은 backend의 Flyway migration이 담당합니다.

### 로컬 DB 생성

repo root 기준 PostgreSQL compose를 사용할 수 있습니다.

```bash
cd infra/compose/postgres
cp .env.example .env
docker compose --env-file .env up -d kyvc-postgres-back-dev
```

Windows PowerShell에서는 `cp` 대신 다음 명령을 사용할 수 있습니다.

```powershell
Copy-Item .env.example .env
```

직접 PostgreSQL에 생성하는 경우 DB 소유자와 애플리케이션 접속 사용자를 맞춥니다.

```sql
CREATE USER kyvc_back_dev WITH PASSWORD 'CHANGE_ME_DEV_POSTGRES_PASSWORD';
CREATE DATABASE kyvc_back OWNER kyvc_back_dev;
```

### 로컬 DB 환경 변수

```env
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
FLYWAY_ENABLED=true
DB_HOST=localhost
DB_PORT=5433
DB_NAME=kyvc_back
DB_USER=kyvc_back_dev
DB_PASSWORD=CHANGE_ME_DEV_POSTGRES_PASSWORD
```

`DB_PASSWORD`는 `infra/compose/postgres/.env`의 `POSTGRES_BACK_DEV_PASSWORD`와 동일해야 합니다.

### Migration 적용

- 위치: `src/main/resources/db/migration`
- 도구: Flyway
- 실행 기준: `FLYWAY_ENABLED=true`
- 실행 시점: `./gradlew bootRun`으로 애플리케이션 시작 시 자동 적용
- 적용 이력 테이블: `flyway_schema_history`
- JPA `ddl-auto`는 `none` 기준이므로 테이블 자동 생성에 의존하지 않습니다.

### Migration 구성

- `V1`: 사용자, 법인, 관리자, 공통코드, 문서요건, AI 심사정책, Issuer 정책 기반 테이블
- `V2`: KYC 신청, KYC 문서, 보완, 심사 이력 테이블
- `V3`: Core 요청, Credential, VP 검증 테이블
- `V4`: 인증 토큰, MFA, 모바일 기기, 감사로그, 알림, 약관 동의 테이블
- `V5`: 초기 관리자, 역할, 공통코드, 정책 seed 데이터
- `V6` 이후: P2P3 지원 컬럼, 법인 문서, Credential 요청, Verifier, 알림 템플릿, DID 기관, Credential Offer, VP 로그인 확장

### 주요 사용 테이블

- 계정: `users`, `roles`, `user_roles`, `admin_users`, `admin_roles`, `admin_user_roles`
- 법인: `corporates`, `corporate_documents`, `corporate_representatives`, `corporate_agents`, `finance_corporate_customers`
- KYC: `kyc_applications`, `kyc_documents`, `kyc_supplements`, `kyc_supplement_documents`, `kyc_review_histories`
- Credential: `credentials`, `credential_requests`, `credential_status_histories`, `credential_offers`
- VP/Verifier: `vp_verifications`, `verifiers`, `verifier_api_keys`, `verifier_callbacks`, `verifier_logs`
- 공통/운영: `common_code_groups`, `common_codes`, `document_requirements`, `ai_review_policies`, `issuer_configs`, `issuer_policies`
- 로그/알림: `audit_logs`, `notifications`, `notification_templates`, `core_requests`, `did_institutions`

### DB 초기화 순서

1. PostgreSQL 컨테이너 실행 또는 DB 수동 생성
2. `backend/.env`에 DB 접속 정보 설정
3. `FLYWAY_ENABLED=true` 확인
4. `cd backend && ./gradlew bootRun` 실행
5. `flyway_schema_history`와 `/health` 응답으로 적용 상태 확인

운영 DB에서는 기존 데이터가 있으면 임의 삭제나 재생성 없이 신규 migration만 추가합니다.

### Repository 구조

- 단순 저장/조회: `XxxRepository` + `XxxRepositoryImpl`
- 목록/검색/통계: `XxxQueryRepository` + `XxxQueryRepositoryImpl`
- JPA 직접 접근은 Repository 구현체 내부로 제한합니다.

## 6. 인증/인가 구조

### JWT

- `JwtTokenProvider`에서 발급과 검증 처리
- Refresh Token 원문 저장 금지
- Refresh Token 저장 시 `TokenHashUtil.sha256(...)` 사용

### Cookie

- Access Token과 Refresh Token은 HttpOnly Cookie 기준
- Cookie 생성과 삭제는 `TokenCookieUtil` 사용

### Role

- 인증 사용자 정보는 `CustomUserDetails` 기준
- 사용자 ID는 `CustomUserDetails.userId` 기준

### 내부 API 인증

- `/api/internal/**` 경로는 내부 API Key와 보안 설정 기준 보호
- 인증/인가 설정은 `SecurityConfig` 기준

## 7. 외부 연동 구조

### Core 연동

- `KYVC_CORE_BASE_URL` 기준 Core API 호출
- Core 요청 이력은 Core 도메인에서 관리

### DB 연동

- PostgreSQL JDBC
- JPA와 Flyway 사용

### 파일 저장소

- 로컬 기본 경로: `./storage`
- KYC 문서 경로: `KYVC_DOCUMENT_STORAGE_PATH`

### 외부 API

- SMTP 메일 서버
- Core 내부 연동 API

## 8. 환경 변수 구조

### 로컬 환경

- 기준 파일: `.env.example`
- 주요 값: `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`, `DB_*`, `KYVC_JWT_*`, `KYVC_CORE_*`, `KYVC_CORS_*`

### dev 환경

- `SPRING_PROFILES_ACTIVE=dev`
- dev DB, dev Core, dev 프론트 도메인 기준 설정

### prod 환경

- `SPRING_PROFILES_ACTIVE=prod`
- 운영 DB, 운영 Core, 보안 Cookie, 운영 CORS 기준 설정

## 9. 실행 구조

### 로컬 실행

```bash
cd backend
./gradlew bootRun
```

### Docker 실행

```bash
cd backend
docker build -t kyvc-backend .
docker run --env-file .env -p 8080:8080 kyvc-backend
```

### 빌드

```bash
cd backend
./gradlew clean build
```

## 10. 개발 규칙

### 디렉터리 작업 범위

- 사용자 서비스 API는 `backend` 내부에서만 작성
- 관리자 API는 `backend_admin`에서 작성
- Core 직접 구현은 `core`에서 작성

### 응답 포맷

- Controller에서 `new CommonResponse(...)` 직접 생성 금지
- `CommonResponseFactory` 사용

### 예외 처리

- `RuntimeException` 직접 throw 금지
- `ApiException`과 `ErrorCode` 사용
- 반복 try-catch 작성 금지
