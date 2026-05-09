# Part 0: V6~V11 DB Migration / backend_admin Gap Analysis

작성일: 2026-05-09
대상 브랜치: `feature/backend-admin-global`

## 1. 프로젝트 구조 확인

- `backend`: 사용자 백엔드 애플리케이션. Flyway migration 위치는 `backend/src/main/resources/db/migration`.
- `backend_admin`: 백어드민 애플리케이션. 현재 `domain/*/{domain,dto,repository,application,controller}` 중심 구조.
- `backend_admin` 주요 구조:
  - Entity: `domain/*/domain`
  - DTO: `domain/*/dto`
  - Repository: `domain/*/repository`, 대부분 `interface + Impl`
  - Service: `domain/*/application`
  - Controller: `domain/*/controller`

## 2. Flyway V6~V11 요약

### V6__p2p3_add_support_columns.sql

기존 테이블 컬럼 추가:

- `users`
  - `user_name varchar(100)`
  - `phone varchar(30)`
  - `notification_enabled_yn char(1) default 'Y'`
  - `mfa_enabled_yn char(1) default 'N'`
  - `mfa_type_code varchar(30)`
  - `last_password_changed_at timestamp`
- `corporates`
  - `established_date date`
  - `corporate_type_code varchar(50)`
  - `website varchar(500)`
  - `representative_name` NOT NULL 해제
- `kyc_applications`
  - `application_channel_code varchar(30)`
  - `finance_institution_code varchar(50)`
  - `finance_branch_code varchar(50)`
  - `finance_staff_user_id bigint`
  - `finance_customer_no varchar(100)`
  - `visited_at timestamp`
- `kyc_documents`
  - `uploaded_by_type_code varchar(30)`
  - `uploaded_by_user_id bigint`
- `credentials`
  - `offer_token_hash varchar(255)`
  - `offer_expires_at timestamp`
  - `offer_used_yn char(1) default 'N'`
  - `holder_did varchar(255)`
  - `holder_xrpl_address varchar(255)`
  - `wallet_saved_at timestamp`
- `vp_verifications`
  - `verifier_id bigint`
  - `finance_institution_code varchar(50)`
  - `request_type_code varchar(30)`
  - `test_yn char(1) default 'N'`
  - `re_auth_yn char(1) default 'N'`
  - `permission_result_json text`
  - `callback_status_code varchar(30)`
  - `callback_sent_at timestamp`
- `notifications`
  - `channel_code varchar(30)`
  - `target_type_code varchar(30)`
  - `target_id bigint`
  - `template_code varchar(100)`
  - `sent_status_code varchar(30)`
  - `sent_at timestamp`
  - `read_at timestamp`
- `audit_logs`
  - `before_value_json text`
  - `after_value_json text`

backend_admin 영향 우선순위: `users`, `corporates`, `kyc_applications`, `kyc_documents`, `credentials`, `audit_logs`, `notifications`.

### V7__p2p3_create_auth_corporate_document_tables.sql

신규 테이블:

- `roles`
- `user_roles`
- `corporate_documents`
- `corporate_representatives`
- `corporate_agents`
- `finance_corporate_customers`
- `document_delete_requests`

주요 제약조건:

- `roles.role_code` unique
- `user_roles(user_id, role_id)` unique
- `corporate_representatives.corporate_id` unique
- FK: 신규 테이블이 `users`, `roles`, `corporates`, `corporate_documents`, `kyc_documents`, `admin_users` 참조

backend_admin 영향 우선순위: 법인 부가정보 조회, 문서 삭제 요청 관리, 역할/권한 조회 확장.

### V8__p2p3_create_credential_verifier_notification_tables.sql

신규 테이블:

- `credential_requests`
- `credential_status_histories`
- `verifiers`
- `verifier_api_keys`
- `verifier_callbacks`
- `verifier_logs`
- `notification_templates`

추가 제약조건:

- `vp_verifications.verifier_id` -> `verifiers.verifier_id` FK
- `notification_templates.template_code` unique

backend_admin 영향 우선순위: Credential 이력, Verifier 관리, 알림 템플릿 관리.

### V9__p2p3_seed_codes_roles_indexes.sql

초기 데이터:

- 공통코드 그룹: `ROLE_TYPE`, `AGENT_AUTHORITY_STATUS`, `FINANCE_CUSTOMER_LINK_STATUS`, `DOCUMENT_DELETE_REQUEST_STATUS`, `CREDENTIAL_REQUEST_TYPE`, `CREDENTIAL_REQUEST_STATUS`, `VERIFIER_STATUS`, `VERIFIER_API_KEY_STATUS`, `VERIFIER_CALLBACK_STATUS`, `CALLBACK_DELIVERY_STATUS`, `VERIFIER_ACTION_TYPE`, `NOTIFICATION_CHANNEL`, `NOTIFICATION_SEND_STATUS`, `APPLICATION_CHANNEL`
- 공통코드 값: 위 그룹별 기본 코드
- 기본 역할: `CORPORATE_USER`, `FINANCE_STAFF`, `VERIFIER_APP`

인덱스:

- `idx_user_roles_user_id`
- `idx_kyc_applications_applicant_status`
- `idx_kyc_applications_finance_customer`
- `idx_kyc_documents_kyc_type`
- `idx_corporate_documents_corporate_type`
- `idx_corporate_representatives_corporate_id`
- `idx_corporate_agents_corporate_id`
- `idx_finance_corporate_customers_corporate_id`
- `idx_document_delete_requests_document_id`
- `idx_document_delete_requests_status`
- `idx_credential_requests_credential_id`
- `idx_credential_requests_status`
- `idx_credential_status_histories_credential_id`
- `idx_verifier_api_keys_verifier_id`
- `idx_verifier_callbacks_verifier_id`
- `idx_verifier_logs_verifier_id`
- `idx_verifier_logs_requested_at`
- `idx_notifications_user_read`
- `idx_notifications_target`
- `idx_audit_logs_target`

### V10__add_onboarding_corporate_name_to_users.sql

- `users.onboarding_corporate_name varchar(255)` 추가

### V11__add_corporate_phone_and_agent_fields.sql

- `corporates.corporate_phone varchar(50)` 추가
- 파일명은 agent fields를 포함하지만 현재 SQL 내용은 `corporate_phone` 추가만 확인됨.

## 3. 실제 DB 적용 여부 확인 SQL

현재 Docker 컨테이너가 실행 중이지 않아 실제 DB는 직접 조회하지 못했다.
아래 SQL로 DB 기동 후 확인한다.

```sql
SELECT *
FROM flyway_schema_history
ORDER BY installed_rank DESC;
```

확인 기준:

- `version` 1부터 11까지 존재
- 모든 행의 `success = true`
- `ORDER BY installed_rank DESC` 기준 첫 행의 `version = '11'`

권장 요약 확인 SQL:

```sql
SELECT
    max(installed_rank) AS max_installed_rank,
    max(version) AS max_version_text,
    bool_and(success) AS all_success,
    count(*) FILTER (WHERE version IN ('1','2','3','4','5','6','7','8','9','10','11')) AS v1_to_v11_count
FROM flyway_schema_history;
```

## 4. 주요 테이블 컬럼 확인 SQL

```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'corporates'
ORDER BY ordinal_position;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'kyc_applications'
ORDER BY ordinal_position;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'kyc_documents'
ORDER BY ordinal_position;

SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'audit_logs'
ORDER BY ordinal_position;
```

## 5. 신규 테이블 존재 여부 확인 SQL

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'roles',
    'user_roles',
    'corporate_documents',
    'corporate_representatives',
    'corporate_agents',
    'finance_corporate_customers',
    'document_delete_requests',
    'credential_requests',
    'credential_status_histories',
    'verifiers',
    'verifier_api_keys',
    'verifier_callbacks',
    'verifier_logs',
    'notification_templates'
  )
ORDER BY table_name;
```

기대 결과: 14개 테이블 모두 반환.

## 6. backend_admin Entity 반영 누락

### User

파일: `backend_admin/src/main/java/com/kyvc/backendadmin/domain/user/domain/User.java`

- 누락: `user_name`
- 누락: `phone`
- 누락: `notification_enabled_yn`
- 누락: `mfa_enabled_yn`
- 누락: `mfa_type_code`
- 누락: `last_password_changed_at`
- 누락: `onboarding_corporate_name`

### Corporate

파일: `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/domain/Corporate.java`

- 누락: `corporate_phone`
- 누락: `established_date`
- 누락: `corporate_type_code`
- 누락: `website`
- 불일치: `representative_name`은 Entity에서 `nullable = false`, DB는 V6에서 nullable 허용

### KycApplication

파일: `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/domain/KycApplication.java`

- 누락: `application_channel_code`
- 누락: `finance_institution_code`
- 누락: `finance_branch_code`
- 누락: `finance_staff_user_id`
- 누락: `finance_customer_no`
- 누락: `visited_at`

### KycDocument

파일: `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/domain/KycDocument.java`

- 누락: `uploaded_by_type_code`
- 누락: `uploaded_by_user_id`

### AuditLog

파일: `backend_admin/src/main/java/com/kyvc/backendadmin/domain/audit/domain/AuditLog.java`

- 누락: `before_value_json`
- 누락: `after_value_json`

참고: `domain/admin/domain/AuditLog.java`도 별도 존재하므로 Part 1에서 중복 엔티티 영향 검토 필요.

### Credential

- JPA Entity 파일은 확인되지 않음.
- Native SQL Repository/DTO 중심 구현.
- V6 추가 컬럼(`offer_token_hash`, `offer_expires_at`, `offer_used_yn`, `holder_did`, `holder_xrpl_address`, `wallet_saved_at`)은 기존 Credential 조회 DTO/쿼리에 반영되지 않음.

## 7. backend_admin DTO 반영 누락

### 법인 사용자 API

대상:

- `GET /api/admin/backend/users`
- `GET /api/admin/backend/users/{userId}`

DTO:

- `AdminCorporateUserListResponse.Item`
- `AdminCorporateUserDetailResponse.UserInfo`

누락 필드:

- `userName`
- `phone`
- `notificationEnabledYn`
- `mfaEnabledYn`
- `mfaTypeCode`
- `lastPasswordChangedAt`
- `onboardingCorporateName`

Repository 쿼리도 해당 컬럼을 select하지 않음.

### 법인 상세 API

대상:

- `GET /api/admin/backend/corporates/{corporateId}`

DTO:

- `AdminCorporateDetailResponse`

누락 필드:

- `corporatePhone`
- `corporateTypeCode`
- `establishedDate`
- `website`

Repository 쿼리도 해당 컬럼을 select하지 않음.

### KYC 신청 API

대상:

- `GET /api/admin/backend/kyc/applications`
- `GET /api/admin/backend/kyc/applications/{kycId}`
- `GET /api/admin/backend/kyc/applications/{kycId}/corporate`

DTO:

- `AdminKycApplicationListResponse.Item`
- `AdminKycApplicationDetailResponse`
- `AdminKycApplicationCorporateResponse`

누락 필드:

- `applicationChannelCode`
- `financeInstitutionCode`
- `financeBranchCode`
- `financeStaffUserId`
- `financeCustomerNo`
- `visitedAt`

Repository 쿼리도 해당 컬럼을 select하지 않음.

### KYC 문서 API

대상:

- `GET /api/admin/backend/kyc/applications/{kycId}/documents`
- `GET /api/admin/backend/kyc/applications/{kycId}/documents/{documentId}/preview`

DTO:

- `AdminKycDocumentListResponse.Item`
- `AdminKycDocumentPreviewResponse`

누락 필드:

- `uploadedByTypeCode`
- `uploadedByUserId`
- `uploadedByUserName`

Repository 쿼리도 `users` 조인으로 업로더명을 조회하지 않음.

### 감사 로그 API

대상:

- `GET /api/admin/backend/audit-logs`
- `GET /api/admin/backend/audit-logs/{auditId}`

DTO:

- `AdminAuditLogResponse`

누락 필드:

- `beforeValueJson`
- `afterValueJson`

Entity와 Service 매핑도 해당 필드를 포함하지 않음.

## 8. 신규 구현 필요 API 확인

아래 API 경로는 `backend_admin/src/main/java/com/kyvc/backendadmin/domain` 기준으로 구현 흔적을 찾지 못했다.

### 필수 구현 후보

법인 부가정보 API:

- `GET /api/admin/backend/corporates/{corporateId}/representatives`
- `GET /api/admin/backend/corporates/{corporateId}/agents`
- `GET /api/admin/backend/corporates/{corporateId}/documents`

문서 삭제 요청 관리 API:

- `GET /api/admin/backend/document-delete-requests`
- `GET /api/admin/backend/document-delete-requests/{requestId}`
- `POST /api/admin/backend/document-delete-requests/{requestId}/approve`
- `POST /api/admin/backend/document-delete-requests/{requestId}/reject`

Credential 이력 API:

- `GET /api/admin/backend/credentials/{credentialId}/requests`
- `GET /api/admin/backend/credentials/{credentialId}/status-histories`

Verifier 관리 API:

- `GET /api/admin/backend/verifiers`
- `GET /api/admin/backend/verifiers/{verifierId}`
- `POST /api/admin/backend/verifiers`
- `PATCH /api/admin/backend/verifiers/{verifierId}/status`
- `GET /api/admin/backend/verifiers/{verifierId}/api-keys`
- `GET /api/admin/backend/verifiers/{verifierId}/logs`

알림 템플릿 / 알림 이력 API:

- `GET /api/admin/backend/notification-templates`
- `GET /api/admin/backend/notification-templates/{templateId}`
- `POST /api/admin/backend/notification-templates`
- `PATCH /api/admin/backend/notification-templates/{templateId}`
- `GET /api/admin/backend/notifications`
- `GET /api/admin/backend/notifications/{notificationId}`

### 이미 구현된 인접 API

- `GET /api/admin/backend/credentials`
- `GET /api/admin/backend/credentials/{credentialId}`
- `POST /api/admin/backend/kyc/applications/{kycId}/credentials/issue`

## Part 0 결과 요약

### 1. Flyway 적용 상태

- 현재 확인된 마지막 버전: 로컬 DB 미기동으로 직접 확인 불가. migration 파일 기준 V11까지 존재.
- V11 적용 여부: DB에서는 미확인. 파일 기준 `V11__add_corporate_phone_and_agent_fields.sql` 존재.
- 문제 여부:
  - 실행 중인 Docker 컨테이너가 없어 실제 `flyway_schema_history` 조회 불가.
  - V11 파일명은 agent fields를 언급하지만 실제 SQL은 `corporates.corporate_phone`만 추가.

### 2. 신규/변경 테이블 요약

- 새로 추가된 테이블:
  - `roles`
  - `user_roles`
  - `corporate_documents`
  - `corporate_representatives`
  - `corporate_agents`
  - `finance_corporate_customers`
  - `document_delete_requests`
  - `credential_requests`
  - `credential_status_histories`
  - `verifiers`
  - `verifier_api_keys`
  - `verifier_callbacks`
  - `verifier_logs`
  - `notification_templates`
- 변경된 기존 테이블:
  - `users`
  - `corporates`
  - `kyc_applications`
  - `kyc_documents`
  - `credentials`
  - `vp_verifications`
  - `notifications`
  - `audit_logs`

### 3. backend_admin Entity 누락 목록

- User: `userName`, `phone`, `notificationEnabledYn`, `mfaEnabledYn`, `mfaTypeCode`, `lastPasswordChangedAt`, `onboardingCorporateName`
- Corporate: `corporatePhone`, `establishedDate`, `corporateTypeCode`, `website`, `representativeName nullable=true` 반영
- KycApplication: `applicationChannelCode`, `financeInstitutionCode`, `financeBranchCode`, `financeStaffUserId`, `financeCustomerNo`, `visitedAt`
- KycDocument: `uploadedByTypeCode`, `uploadedByUserId`
- AuditLog: `beforeValueJson`, `afterValueJson`
- Credential: Entity 없음. Native 조회 DTO/쿼리에 V6 Credential 추가 컬럼 미반영

### 4. backend_admin DTO 누락 목록

- 법인 사용자 API: `userName`, `phone`, `notificationEnabledYn`, `mfaEnabledYn`, `mfaTypeCode`, `lastPasswordChangedAt`, `onboardingCorporateName`
- 법인 상세 API: `corporatePhone`, `corporateTypeCode`, `establishedDate`, `website`
- KYC 신청 API: `applicationChannelCode`, `financeInstitutionCode`, `financeBranchCode`, `financeStaffUserId`, `financeCustomerNo`, `visitedAt`
- KYC 문서 API: `uploadedByTypeCode`, `uploadedByUserId`, `uploadedByUserName`
- 감사 로그 API: `beforeValueJson`, `afterValueJson`

### 5. 기존 P1 API 수정 필요 목록

- API 경로: `GET /api/admin/backend/users`
  - 수정 사유: V6/V10 사용자 추가 컬럼 응답 누락
  - 관련 테이블: `users`, `corporates`, `kyc_applications`
  - 수정 DTO: `AdminCorporateUserListResponse.Item`
- API 경로: `GET /api/admin/backend/users/{userId}`
  - 수정 사유: V6/V10 사용자 추가 컬럼 응답 누락
  - 관련 테이블: `users`, `corporates`, `kyc_applications`
  - 수정 DTO: `AdminCorporateUserDetailResponse.UserInfo`
- API 경로: `GET /api/admin/backend/corporates/{corporateId}`
  - 수정 사유: V6/V11 법인 추가 컬럼 응답 누락
  - 관련 테이블: `corporates`, `users`, `kyc_applications`
  - 수정 DTO: `AdminCorporateDetailResponse`
- API 경로: `GET /api/admin/backend/kyc/applications`
  - 수정 사유: V6 KYC 금융/채널/방문 컬럼 응답 누락
  - 관련 테이블: `kyc_applications`, `corporates`, `users`
  - 수정 DTO: `AdminKycApplicationListResponse.Item`
- API 경로: `GET /api/admin/backend/kyc/applications/{kycId}`
  - 수정 사유: V6 KYC 금융/채널/방문 컬럼 응답 누락
  - 관련 테이블: `kyc_applications`, `corporates`, `credentials`, `core_requests`
  - 수정 DTO: `AdminKycApplicationDetailResponse`
- API 경로: `GET /api/admin/backend/kyc/applications/{kycId}/corporate`
  - 수정 사유: V6 KYC 금융/채널/방문 컬럼 응답 누락
  - 관련 테이블: `kyc_applications`, `corporates`, `users`
  - 수정 DTO: `AdminKycApplicationCorporateResponse`
- API 경로: `GET /api/admin/backend/kyc/applications/{kycId}/documents`
  - 수정 사유: V6 문서 업로더 컬럼 응답 누락
  - 관련 테이블: `kyc_documents`, `users`, `common_codes`
  - 수정 DTO: `AdminKycDocumentListResponse.Item`
- API 경로: `GET /api/admin/backend/kyc/applications/{kycId}/documents/{documentId}/preview`
  - 수정 사유: V6 문서 업로더 컬럼 응답 누락
  - 관련 테이블: `kyc_documents`, `users`, `common_codes`
  - 수정 DTO: `AdminKycDocumentPreviewResponse`
- API 경로: `GET /api/admin/backend/audit-logs`
  - 수정 사유: V6 감사 로그 before/after JSON 응답 누락
  - 관련 테이블: `audit_logs`
  - 수정 DTO: `AdminAuditLogResponse`
- API 경로: `GET /api/admin/backend/audit-logs/{auditId}`
  - 수정 사유: V6 감사 로그 before/after JSON 응답 누락
  - 관련 테이블: `audit_logs`
  - 수정 DTO: `AdminAuditLogResponse`

### 6. 신규 구현 필요 API 목록

- 필수 구현:
  - 법인 부가정보 API: representatives, agents, documents
  - 문서 삭제 요청 관리 API
  - Credential 요청/상태 이력 API
  - Verifier 목록/상세/생성/상태/API Key/로그 API
  - 알림 템플릿 API
- 여유 있으면 구현:
  - 알림 이력 API: `GET /api/admin/backend/notifications`, `GET /api/admin/backend/notifications/{notificationId}`
  - Verifier callback 조회/관리 API는 DB 테이블이 있으나 이번 목록에는 명시 조회 API가 없으므로 별도 요구사항 확인 필요

### 7. 다음 Part 1에서 해야 할 작업

- `KyvcEnums.java` 수정
  - `APPLICATION_CHANNEL`, `ROLE_TYPE`, `DOCUMENT_DELETE_REQUEST_STATUS`, `CREDENTIAL_REQUEST_TYPE`, `CREDENTIAL_REQUEST_STATUS`, `VERIFIER_STATUS`, `VERIFIER_API_KEY_STATUS`, `VERIFIER_CALLBACK_STATUS`, `VERIFIER_ACTION_TYPE`, `NOTIFICATION_CHANNEL`, `NOTIFICATION_SEND_STATUS` 등 enum 또는 공통코드 처리 기준 정리
- Entity 컬럼 추가
  - `User`, `Corporate`, `KycApplication`, `KycDocument`, `AuditLog`
  - Credential은 Entity 신규 도입 여부 또는 기존 Native DTO 유지 여부 결정
- DTO 응답 필드 추가
  - record 필드 추가 및 Swagger `@Schema` 설명 보강
- Repository 조회 쿼리 수정 준비
  - Native SQL select 컬럼 추가
  - DTO 생성자 인자 순서 변경 반영
  - KYC 문서는 `uploaded_by_user_id` -> `users.user_id` left join으로 `uploadedByUserName` 도출
  - AuditLog는 Entity/JPQL 기반이므로 Entity 필드 추가 후 Service 매핑 수정

