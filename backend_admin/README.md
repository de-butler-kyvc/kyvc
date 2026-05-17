# KYvC Backend Admin Service

## 1. 서비스 개요

### 담당 사용자

- 백엔드 업무 관리자
- 시스템 관리자

### 호출 주체

- `frontend_admin`

### 서비스 역할

KYC 업무 심사와 운영을 위한 관리자 API 서버이다. 관리자 인증, KYC 신청 조회, 제출서류 조회, AI 심사 결과 조회, 수동심사, 보완요청, VC/VP 업무 상태 조회, 법인 사용자와 운영 정책 관리를 담당한다.

### 서비스 도메인

| 도메인 | 환경 | 대상 서비스 |
| --- | --- | --- |
| `dev-admin-api-kyvc.khuoo.synology.me` | dev admin | Synology DSM Reverse Proxy / Backend Admin API |

### Swagger 주소

| 환경  | 주소                                                           |
| ----- | -------------------------------------------------------------- |
| local | `http://localhost:8080/swagger-ui.html`                        |
| dev   | `https://dev-admin-api-kyvc.khuoo.synology.me/swagger-ui.html` |

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
- Flyway 설정 포함, 기본값 `FLYWAY_ENABLED=false`

### 주요 라이브러리

- `springdoc-openapi-starter-webmvc-ui`
- `jjwt`
- `spring-boot-starter-validation`
- `spring-boot-starter-mail`
- `spring-dotenv`
- OpenPDF
- Lombok
- PostgreSQL JDBC Driver

## 3. API 구조

### API 그룹

| Prefix                  | 주요 역할                                                        |
| ----------------------- | ---------------------------------------------------------------- |
| `/api/admin/auth/**`    | 관리자 로그인, 로그아웃, 토큰 재발급, MFA, 비밀번호 재설정, 세션 |
| `/api/admin/me/**`      | 관리자 본인 정보, 비밀번호 변경                                  |
| `/api/admin/backend/**` | backend 업무 관리자 API                                          |
| `/health`               | 헬스체크                                                         |

### 주요 API

| 영역 | Method | Path | 용도 |
| --- | --- | --- | --- |
| 관리자 인증 | POST | `/api/admin/auth/login` | 관리자 로그인 |
| 관리자 인증 | GET | `/api/admin/auth/session` | 관리자 세션 확인 |
| 관리자 본인 정보 | GET | `/api/admin/me` | 관리자 본인 정보 조회 |
| 관리자 본인 정보 | PATCH | `/api/admin/me/password` | 관리자 비밀번호 변경 |
| 대시보드 | GET | `/api/admin/backend/dashboard` | 관리자 대시보드 조회 |
| KYC 신청 | GET | `/api/admin/backend/kyc/applications` | KYC 신청 목록 조회 |
| KYC 신청 | GET | `/api/admin/backend/kyc/applications/{kycId}` | KYC 신청 상세 조회 |
| 제출서류 | GET | `/api/admin/backend/kyc/applications/{kycId}/documents` | 제출서류 목록 조회 |
| 제출서류 | GET | `/api/admin/backend/kyc/applications/{kycId}/documents/{documentId}/preview` | 제출서류 미리보기 |
| AI 심사 | GET | `/api/admin/backend/kyc/applications/{kycId}/ai-review` | AI 심사 결과 조회 |
| 수동심사 | POST | `/api/admin/backend/kyc/applications/{kycId}/manual-review/approve` | KYC 승인 처리 |
| 수동심사 | POST | `/api/admin/backend/kyc/applications/{kycId}/manual-review/reject` | KYC 반려 처리 |
| 보완요청 | POST | `/api/admin/backend/kyc/applications/{kycId}/supplements` | 보완요청 등록 |
| Credential | GET | `/api/admin/backend/credentials` | VC 발급 상태 목록 조회 |
| Credential | GET | `/api/admin/backend/credentials/{credentialId}` | VC 발급 상태 상세 조회 |
| VP 검증 | GET | `/api/admin/backend/vp-verifications` | VP 검증 결과 목록 조회 |
| VP 검증 | GET | `/api/admin/backend/vp-verifications/{verificationId}` | VP 검증 결과 상세 조회 |
| 사용자/법인 | GET | `/api/admin/backend/users` | 사용자 목록 조회 |
| 사용자/법인 | GET | `/api/admin/backend/corporates/{corporateId}` | 법인 상세 조회 |
| Issuer 정책 | GET | `/api/admin/backend/issuer-policies` | Issuer 신뢰정책 목록 조회 |
| Issuer 정책 | POST | `/api/admin/backend/issuer-policies/whitelist` | Issuer whitelist 등록 |
| Issuer 정책 | POST | `/api/admin/backend/issuer-policies/blacklist` | Issuer blacklist 등록 |
| 감사로그 | GET | `/api/admin/backend/audit-logs` | 감사로그 조회 |

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

- 관리자 API도 `success`, `code`, `message`, `data` 구조를 따른다.
- Controller에서 `new CommonResponse(...)` 직접 생성은 금지한다.
- 비즈니스 예외는 `ApiException` + `ErrorCode`를 사용한다.

## 4. 패키지 구조

### global

- `config`: Swagger 등 전역 설정
- `exception`: `ApiException`, `ErrorCode`, `GlobalExceptionHandler`
- `response`: `CommonResponse`, `CommonResponseFactory`
- `security`, `jwt`: 관리자 Spring Security, JWT, Role 권한
- `logging`, `filter`: 요청 ID, API 로그, 업무 이벤트 로그
- `commoncode`, `mail`, `util`: 공통코드, 메일, enum 유틸

### domain

- `admin`, `auth`, `dashboard`
- `kyc`, `document`, `review`
- `credential`, `vp`, `issuer`, `verifier`, `corporate`
- `audit`, `security`, `notification`, `report`, `core`, `user`

### resources

- `application.yml`
- `application-local.yml`
- `application-dev.yml`
- `application-prod.yml`
- `logback-spring.xml`
- `db/migration`은 현재 backend_admin 리소스에서 확인되지 않음

## 5. DB 구조

### 주요 사용 테이블

| 구분          | 테이블                                                                                                                                                    |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 관리자/인증   | `admin_users`, `admin_roles`, `admin_user_roles`, `auth_tokens`, `mfa_email_verifications`                                                                |
| 사용자/법인   | `users`, `corporates`                                                                                                                                     |
| KYC/문서/심사 | `kyc_applications`, `kyc_documents`, `kyc_supplements`, `kyc_review_histories`, `document_requirements`, `document_delete_requests`, `ai_review_policies` |
| Credential/VP | `credentials`, `credential_requests`, `credential_status_histories`, `credential_offers`, `vp_verifications`                                              |
| 정책/운영     | `issuer_policies`, `issuer_configs`, `common_code_groups`, `common_codes`, `audit_logs`, `notifications`, `notification_templates`                        |

## 6. 인증/인가 구조

### JWT

- 관리자 JWT는 `JwtTokenProvider`에서 발급/검증한다.
- 관리자 인증 사용자 정보는 `CustomUserDetails` 기준이다.
- Refresh Token 원문 저장은 금지하고 hash 저장 기준을 따른다.

### Cookie

- Access Token과 Refresh Token은 HttpOnly Cookie 기준으로 전달한다.
- Cookie 생성과 삭제는 `TokenCookieUtil` 기준이다.
- 로그인 응답 body에 토큰 원문을 포함하지 않는다.

### Role

| 권한          | 처리 기준                                                                                       |
| ------------- | ----------------------------------------------------------------------------------------------- |
| 일반 관리자   | 금융사 데스크 업무, AI 수동검토, QR 생성, 방문자 정보 입력, 제출서류 업로드 등 제한된 업무 처리 |
| 시스템 관리자 | 신청목록 조회, 로그조회, 운영/보안/권한 등 시스템 전반 관리                                     |

- 코드 기준 주요 Role은 `BACKEND_ADMIN`, `SYSTEM_ADMIN`, `OPERATOR`, `AUDITOR`이다.
- 감사/리포트, 시스템 설정, 관리자 계정, 정책 API는 Role별 접근 범위를 분리한다.

### 내부 API 인증

- backend_admin은 내부 Core API를 직접 호출하지 않는다.
- `/api/admin/core/**` 요청은 보안 정책에서 금지한다.
- Core 기술 운영 API 인증은 core_admin 영역 기준이다.

## 7. 외부 연동 구조

### Core 연동

- backend_admin에서 Core 직접 연동은 금지한다.
- backend_admin은 `core`, `core_admin`, `back-core-admin`을 직접 호출하지 않는다.
- Core AI 결과, VC 발급 상태, VP 검증 결과는 backend 업무 DB에 동기화된 값을 조회한다.
- `CoreAdapter` 직접 호출 구조를 backend_admin에 추가하지 않는다.

### DB 연동

- backend 업무 DB 기준으로 조회한다.
- PostgreSQL JDBC와 JPA Repository 구현체 기준으로 접근한다.

### 파일 저장소

- 제출서류 미리보기/조회는 저장소 정책에 따른다.
- backend_admin 컨테이너는 문서 저장소를 읽기 전용으로 마운트하는 기준이다.
- 민감 원문 노출 권한은 관리자 Role과 업무 범위로 제한한다.

### 외부 API

- 원칙적으로 Core 직접 호출은 없다.
- SMTP 메일 서버는 관리자 인증/MFA 등 필요한 경우에만 사용한다.
- 외부 연동 결과는 backend 업무 DB 기준으로 조회한다.

## 8. 주요 환경 변수

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: 실행 환경의 PostgreSQL 접속 정보로 변경
- `CORE_ADMIN_BASE_URL`: core_admin API URL로 변경
- `KYVC_JWT_SECRET`: 운영에서는 예시값이 아닌 충분히 긴 비밀키로 변경
- `KYVC_CORS_ALLOWED_ORIGINS`: 실제 관리자 프론트 도메인으로 변경
- `KYVC_MAIL_HOST`, `KYVC_MAIL_USERNAME`, `KYVC_MAIL_PASSWORD`, `KYVC_MAIL_FROM`: 관리자 인증/MFA 메일 발송 계정 정보로 변경
- `APP_STORAGE_PATH`, `LOG_PATH`, `SERVICE_LOG_PATH`: 서버 저장소와 로그 마운트 경로로 변경

## 9. 실행 구조

### 로컬 실행

```bash
cd backend_admin
./gradlew bootRun
```

Windows PowerShell 기준:

```powershell
cd backend_admin
.\gradlew.bat bootRun
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

### 작업 경계

| 구분                | 기준                                                            |
| ------------------- | --------------------------------------------------------------- |
| 수정 범위           | `backend_admin` 작업은 `backend_admin` 디렉터리 내부에서만 수행 |
| 사용자 업무 API     | `backend` 영역에서 구현                                         |
| Core 기술 운영 API  | `core_admin` 영역에서 구현                                      |
| Core 기술 운영 화면 | `frontend_core_admin` 영역에서 구현                             |

### API 응답 및 모델링

| 영역         | 필수 기준                               | 금지 또는 주의                                               |
| ------------ | --------------------------------------- | ------------------------------------------------------------ |
| API 응답     | 모든 API 응답은 `CommonResponse` 기준   | Controller에서 응답 포맷 임의 구성 금지                      |
| 응답 생성    | `CommonResponseFactory` 사용            | `new CommonResponse(...)` 직접 생성 금지                     |
| 관리자 응답  | 관리자 업무에 필요한 가공 데이터만 반환 | `coreRequestId`, Core trace, Core raw payload 직접 노출 금지 |
| DTO/Response | Java record 사용                        | DTO record에 Lombok 사용 금지                                |
| Entity       | 일반 class 사용                         | Entity를 record로 작성 금지                                  |

### 예외, 권한, 로깅

| 영역          | 필수 기준                                 | 금지 또는 주의                                                                                                                                |
| ------------- | ----------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| Core 연동     | backend_admin 도메인 서비스 기준으로 처리 | `CoreAdapter` 직접 호출 금지                                                                                                                  |
| 비즈니스 예외 | `ApiException` + `ErrorCode` 사용         | `RuntimeException` 직접 throw 금지                                                                                                            |
| 예외 응답     | `GlobalExceptionHandler` 기준 처리        | 반복 try-catch 작성 금지                                                                                                                      |
| 관리자 권한   | 역할별 API 호출 가능 범위 분리            | 권한 경계가 모호한 공용 API 작성 금지                                                                                                         |
| 일반 로그     | Slf4j 또는 프로젝트 로깅 구조 사용        | `System.out.println` 사용 금지                                                                                                                |
| 민감정보 로그 | 필요한 식별자만 최소 기록                 | password, token, accessToken, refreshToken, authorization, cookie, jwt, secret, privateKey, VC/VP 원문, 문서 원문, Core raw payload 출력 금지 |
