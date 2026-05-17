# KYvC Backend Service

## 1. 서비스 개요

### 담당 사용자

- 법인 사용자
- 모바일 앱 사용자
- 외부 Verifier 연동 주체

### 호출 주체

- `frontend`
- 모바일 앱
- 외부 Verifier
- 내부 시스템

### 서비스 역할

KYvC 사용자 업무 API 서버이다. KYC 신청, 문서 업로드, 업무 상태 관리, Core 요청 생성, Core 응답 기반 업무 DB 동기화를 담당한다.

### 서비스 도메인

| 도메인 | 환경 | 대상 서비스 |
| --- | --- | --- |
| `dev-api-kyvc.khuoo.synology.me` | dev | Synology DSM Reverse Proxy / Backend API |

### Swagger 주소

| 환경 | 주소 |
| --- | --- |
| local | `http://localhost:8080/swagger-ui/index.html` |
| dev | `https://dev-api-kyvc.khuoo.synology.me/swagger-ui/index.html` |

## 2. 기술 스택

### 언어

- Java 21

### 프레임워크

- Spring Boot 3.5.13
- Spring MVC
- Spring Security
- Spring Data JPA
- Spring WebFlux WebClient

### 빌드 도구

- Gradle Wrapper

### 데이터베이스

- PostgreSQL
- Flyway Migration

### 주요 라이브러리

- `springdoc-openapi-starter-webmvc-ui`
- `jjwt`
- `spring-boot-starter-validation`
- `spring-boot-starter-mail`
- Lombok
- PostgreSQL JDBC Driver

## 3. API 구조

### API 그룹

| Prefix | 주요 역할 |
| --- | --- |
| `/api/auth/**` | 회원가입, 로그인, 로그아웃, 토큰 재발급, MFA, 이메일 인증, 비밀번호 재설정 |
| `/api/common/**` | 세션, 공통코드, 알림, DID 기관 조회 |
| `/api/user/**` | 사용자 대시보드, 법인 정보, 문서, Credential, VP 이력 |
| `/api/corporate/**` | 법인 KYC 신청, 제출서류, 보완 제출, VC 발급 안내 |
| `/api/mobile/**` | 모바일 인증, 기기 등록, QR 해석, VP 제출, 모바일 지갑 Credential |
| `/api/finance/**` | 금융사 KYC, VP 요청, Verifier 연동 |
| `/api/verifier/**` | Verifier 재인증 요청, 권한, 테스트 VP 검증, 사용 통계 |
| `/api/internal/**` | 내부 헬스체크, Core 헬스체크, 알림, 감사로그, 개발용 내부 처리 |

### 주요 API

- 법인 회원가입 전 이메일 인증: `POST /api/auth/email-verifications/request`, `POST /api/auth/email-verifications/verify`
- 법인 회원가입과 로그인: `POST /api/auth/signup/corporate`, `POST /api/auth/login`
- 세션 확인과 토큰 재발급: `GET /api/common/session`, `POST /api/auth/token/refresh`
- 법인 정보 등록과 조회: `POST /api/user/corporates`, `GET /api/user/corporates/me`
- KYC 신청 생성/조회/제출: `POST /api/corporate/kyc/applications`, `GET /api/corporate/kyc/applications/current`, `POST /api/corporate/kyc/applications/{kycId}/submit`
- 제출서류 업로드와 미리보기: `POST /api/corporate/kyc/applications/{kycId}/documents`, `GET /api/corporate/kyc/applications/{kycId}/documents/{documentId}/preview`
- 보완 제출: `GET /api/corporate/kyc/applications/{kycId}/supplements`, `POST /api/corporate/kyc/applications/{kycId}/supplements/{supplementId}/submit`
- Credential 조회와 Offer 상태 조회: `GET /api/user/credentials`, `GET /api/user/credential-offers/{offerId}/status`
- 모바일 VP 제출: `POST /api/mobile/qr/resolve`, `POST /api/mobile/vp/presentations`
- 금융사 VP 요청: `POST /api/finance/verifier/vp-requests`, `GET /api/finance/verifier/vp-requests/{requestId}`

### 요청/응답 규칙

모든 API 응답은 `CommonResponse` 기준이며 Controller 응답 생성은 `CommonResponseFactory`를 사용한다.

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "요청 성공",
  "data": {}
}
```

- 성공/실패 여부는 `success`로 구분한다.
- 업무 코드와 메시지는 `code`, `message`로 전달한다.
- 실제 응답 본문은 `data`에 담는다.
- Controller에서 `new CommonResponse(...)` 직접 생성은 금지한다.

## 4. 패키지 구조

### global

- `config`: Swagger 등 전역 설정
- `exception`: `ApiException`, `ErrorCode`, `GlobalExceptionHandler`
- `response`: `CommonResponse`, `CommonResponseFactory`
- `security`, `jwt`: Spring Security, JWT, Cookie, 내부 API Key
- `util`: `KyvcEnums` 등 공통 유틸
- `logging`, `filter`: 요청 ID, API 로그, 업무 이벤트 로그
- `mail`: SMTP 메일 발송 공통 영역

### domain

- `auth`, `user`, `corporate`, `kyc`, `document`, `review`
- `credential`, `vp`, `verifier`, `finance`, `mobile`
- `notification`, `audit`, `commoncode`, `did`, `issuer`, `core`

### resources

- `application.yml`
- `application-local.yml`
- `application-dev.yml`
- `application-prod.yml`
- `db/migration`
- `logback-spring.xml`

## 5. DB 구조

### 주요 사용 테이블

| 구분 | 테이블 |
| --- | --- |
| 사용자/인증 | `users`, `auth_tokens`, `mfa_email_verifications`, `user_consents` |
| 법인 | `corporates`, `corporate_documents`, `corporate_representatives`, `corporate_agents`, `finance_corporate_customers` |
| KYC/문서/심사 | `kyc_applications`, `kyc_documents`, `kyc_supplements`, `kyc_supplement_documents`, `kyc_review_histories`, `document_requirements`, `document_delete_requests` |
| Credential | `credentials`, `credential_requests`, `credential_status_histories`, `credential_offers` |
| VP/Verifier | `vp_verifications`, `verifiers`, `verifier_api_keys`, `verifier_callbacks`, `verifier_logs` |
| 운영/공통 | `common_code_groups`, `common_codes`, `issuer_configs`, `issuer_policies`, `did_institutions`, `notifications`, `notification_templates`, `audit_logs`, `core_requests` |

### Migration (backend만 작성)

- 위치: `backend/src/main/resources/db/migration`
- 도구: Flyway
- 실행 기준: `FLYWAY_ENABLED=true`
- 적용 이력 테이블: `flyway_schema_history`

| 파일 | 역할 |
| --- | --- |
| `V1__create_base_identity_admin_policy_tables.sql` | 사용자, 관리자, 역할, 공통 정책 기반 테이블 |
| `V2__create_kyc_business_tables.sql` | KYC, 문서, 보완, 심사 업무 테이블 |
| `V3__create_core_credential_vp_tables.sql` | Core 요청, Credential, VP 테이블 |
| `V4__create_auth_mobile_audit_notification_tables.sql` | 인증 토큰, MFA, 모바일, 감사로그, 알림 테이블 |
| `V5__insert_initial_codes_roles_policies.sql` | 초기 코드, 역할, 정책 seed 데이터 |

### Repository 구조

- 단순 저장/조회는 `XxxRepository` + `XxxRepositoryImpl` 구조를 사용한다.
- 목록/조건/Join/통계 조회는 `XxxQueryRepository` + `XxxQueryRepositoryImpl` 구조를 사용한다.
- Service에서 JPA Repository 직접 사용은 금지한다.
- Repository 관련 파일은 각 도메인의 `repository` 패키지에 작성한다.

## 6. 인증/인가 구조

### JWT

- `JwtTokenProvider`에서 Access Token과 Refresh Token 발급/검증을 처리한다.
- Access Token은 API 접근에 사용한다.
- Refresh Token은 재발급에 사용하며 원문 저장은 금지한다.
- Refresh Token 저장은 `TokenHashUtil.sha256(...)` 기준이다.

### Cookie

- Access Token과 Refresh Token은 HttpOnly Cookie 기준으로 전달한다.
- Cookie 생성과 삭제는 `TokenCookieUtil`을 사용한다.
- 로그인 응답 body에 토큰 원문을 포함하지 않는다.

### Role

- 인증 사용자 정보는 `CustomUserDetails` 기준이다.
- SecurityContext에서 사용자 ID가 필요하면 `CustomUserDetails.userId`를 사용한다.
- 사용자, 모바일, 관리자 권한은 분리한다.
- backend에서는 `/api/admin/**` 구현과 접근을 제외한다.

### 내부 API 인증

- `/api/internal/core/**`, `/api/internal/dev/**`, `/api/internal/issuer-policies/**`, `/api/internal/notifications/**`는 내부 API Key 기준으로 보호한다.
- 내부 API Key Header는 `X-Internal-Api-Key` 기준이다.
- `/api/internal/core/health`는 헬스체크용 공개 GET 요청으로 허용한다.
- 운영 환경에서는 내부 네트워크 제한과 API Key 설정을 함께 적용한다.

## 7. 외부 연동 구조

### Core 연동

- backend만 `CoreHttpAdapter`를 통해 core와 통신한다.
- Core 호출 기준 URL은 `KYVC_CORE_BASE_URL`이다.
- 현재 기준은 callback 신규 구현이 아니라 동기/long-running 방식이다.
- Java 21 Virtual Thread 설정(`spring.threads.virtual.enabled=true`)으로 blocking 대기 비용을 완화한다.
- Core raw payload, VC/VP 원문, 문서 원문은 로그에 남기지 않는다.

### DB 연동

- PostgreSQL JDBC와 Spring Data JPA 기준이다.
- 기본 datasource는 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` 환경변수 기준이다.
- JPA `ddl-auto`는 `none` 기준이며 스키마 관리는 Flyway 기준이다.

### 파일 저장소

- 문서 저장소 경로는 `KYVC_DOCUMENT_STORAGE_PATH` 기준이다.
- 컨테이너 기본 저장 경로는 `/app/storage/kyc-documents` 기준이다.
- 로컬 기본 경로는 프로젝트 환경에 맞게 설정한다.

### 외부 API

- Core API
- SMTP 메일 서버
- 외부 Verifier 콜백/연동 API
- XRPL 직접 연동은 core 담당이며 backend는 Core API 결과를 업무 DB에 동기화한다.

## 8. 주요 환경 변수

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: 실행 환경의 PostgreSQL 접속 정보로 변경
- `KYVC_CORE_BASE_URL`: dev/prod Core API URL로 변경
- `KYVC_CORE_API_KEY`: Core 내부 API Key 사용 시 실제 값으로 변경
- `KYVC_JWT_SECRET`: 운영에서는 예시값이 아닌 충분히 긴 비밀키로 변경
- `KYVC_CORS_ALLOWED_ORIGINS`: 실제 frontend, admin 도메인으로 변경
- `KYVC_MAIL_HOST`, `KYVC_MAIL_USERNAME`, `KYVC_MAIL_PASSWORD`, `KYVC_MAIL_FROM`: 메일 발송 계정 정보로 변경
- `KYVC_PASSWORD_RESET_BASE_URL`: 실제 비밀번호 재설정 화면 URL로 변경
- `KYVC_DOCUMENT_STORAGE_PATH`: 서버 문서 저장소 경로로 변경

## 9. 실행 구조

### 로컬 실행

```bash
cd backend
./gradlew bootRun
```

Windows PowerShell 기준:

```powershell
cd backend
.\gradlew.bat bootRun
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

### 작업 경계

| 구분 | 기준 |
| --- | --- |
| 수정 범위 | `backend` 작업은 `backend` 디렉터리 내부에서만 수행 |
| 제외 범위 | `backend_admin`, `frontend`, `frontend_admin`, `core`, `core_admin`, `infra`, `.github` 수정 금지 |
| 관리자 기능 | admin 패키지와 backend-admin 관련 코드는 `backend`에 생성하지 않음 |
| 업무 코드 | 사용자 업무 API는 `domain` 하위, 공통 코드는 `global` 하위 기준 |

### API 응답 및 모델링

| 영역 | 필수 기준 | 금지 또는 주의 |
| --- | --- | --- |
| API 응답 | 모든 API 응답은 `CommonResponse` 기준 | Controller에서 응답 포맷 임의 구성 금지 |
| 응답 생성 | `CommonResponseFactory` 사용 | `new CommonResponse(...)` 직접 생성 금지 |
| DTO/Response | Java record 사용 | DTO record에 Lombok 사용 금지 |
| Entity | 일반 class 사용 | Entity를 record로 작성 금지 |
| 상태값 | `KyvcEnums` 또는 공통코드 기준 사용 | 상태값 문자열 직접 비교 금지 |

### 예외, 로깅, 보안

| 영역 | 필수 기준 | 금지 또는 주의 |
| --- | --- | --- |
| 비즈니스 예외 | `ApiException` + `ErrorCode` 사용 | `RuntimeException` 직접 throw 금지 |
| 예외 응답 | `GlobalExceptionHandler` 기준 처리 | 반복 try-catch 작성 금지 |
| 업무 로그 | `LogEventLogger` 사용 | 직접 JSON 로그 문자열 조립 금지 |
| 일반 로그 | Slf4j 사용 | `System.out.println` 사용 금지 |
| 민감정보 로그 | 필요한 식별자만 최소 기록 | password, token, accessToken, refreshToken, authorization, cookie, jwt, secret, privateKey, VC/VP 원문, 문서 원문, Core raw payload 출력 금지 |
