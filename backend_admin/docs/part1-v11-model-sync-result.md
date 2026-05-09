# Part 1: Enum / Entity / DTO Synchronization Result

작성일: 2026-05-09
브랜치: `feature/backend-admin-global`

## 1. 수정한 파일 목록

- `backend_admin/src/main/java/com/kyvc/backendadmin/global/util/KyvcEnums.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/user/domain/User.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/domain/Corporate.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/domain/KycApplication.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/domain/KycDocument.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/audit/domain/AuditLog.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/dto/AdminCorporateUserListResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/dto/AdminCorporateUserDetailResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/dto/AdminCorporateDetailResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/dto/AdminKycApplicationListResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/dto/AdminKycApplicationDetailResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/dto/AdminKycApplicationCorporateResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminKycDocumentListResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminKycDocumentPreviewResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/audit/dto/AdminAuditLogResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/repository/CorporateQueryRepositoryImpl.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/repository/KycApplicationQueryRepositoryImpl.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/repository/KycDocumentQueryRepository.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/repository/KycDocumentQueryRepositoryImpl.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/document/application/AdminKycDocumentService.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/audit/application/AdminAuditLogQueryService.java`

## 2. KyvcEnums.java 변경 내용

V9 migration의 common_codes 값을 기준으로 enum을 추가했다.

- `ApplicationChannel`: `WEB`, `MOBILE`, `FINANCE_BRANCH`, `ONLINE`, `FINANCE_VISIT`
- `UploadActorType`: `USER`, `ADMIN`, `SYSTEM`
- `DocumentDeleteRequestStatus`: `REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED`, `CANCELLED`
- `CredentialRequestType`: `ISSUE`, `REVOKE`, `STATUS_CHECK`, `REISSUE`
- `CredentialRequestStatus`: `REQUESTED`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELLED`
- `VerifierStatus`: `PENDING`, `ACTIVE`, `SUSPENDED`, `REJECTED`, `APPROVED`
- `VerifierApiKeyStatus`: `ACTIVE`, `REVOKED`, `EXPIRED`, `ROTATED`
- `VerifierCallbackStatus`: `ACTIVE`, `INACTIVE`, `DISABLED`
- `CallbackDeliveryStatus`: `PENDING`, `SUCCESS`, `FAILED`, `SENT`
- `NotificationChannel`: `IN_APP`, `EMAIL`, `WEB`, `APP_PUSH`, `SMS`
- `NotificationSendStatus`: `PENDING`, `SENT`, `FAILED`, `READY`, `CANCELLED`
- `VerifierActionType`: `VP_REQUEST`, `VP_VERIFY`, `API_KEY_ISSUE`, `API_CALL`, `POLICY_SYNC`, `RE_AUTH`, `TEST_VERIFY`, `USAGE_EXPORT`

## 3. Entity 변경 내용

- `User`: V6/V10 사용자 컬럼 추가
- `Corporate`: V6/V11 법인 컬럼 추가, `representativeName` nullable 제약 제거
- `KycApplication`: V6 신청 채널/금융기관/방문 컬럼 추가
- `KycDocument`: V6 업로드 주체 컬럼 추가
- `AuditLog`: 감사 로그 API에서 사용하는 `domain.audit.domain.AuditLog`에 `beforeValueJson`, `afterValueJson` 추가

## 4. DTO 변경 내용

- 법인 사용자 목록/상세 DTO에 사용자 이름, 연락처, 알림/MFA, 비밀번호 변경일, 온보딩 법인명 추가
- 법인 상세 DTO에 법인 전화, 법인 유형, 설립일, 웹사이트 추가
- KYC 신청 목록/상세/법인정보 DTO에 신청 채널, 금융기관, 지점, 담당자, 고객번호, 방문일 추가
- KYC 문서 목록/미리보기 DTO에 업로드 주체 유형, 업로드 사용자 ID, 업로드 사용자명 추가
- 감사 로그 DTO에 변경 전/후 JSON 추가

## 5. Repository / Query 변경 내용

- `CorporateQueryRepositoryImpl`: 사용자/법인 신규 컬럼 select 및 record 생성자 매핑 반영
- `KycApplicationQueryRepositoryImpl`: KYC 신규 컬럼 select 및 record 생성자 매핑 반영
- `KycDocumentQueryRepositoryImpl`: `uploaded_by_user_id` 기준 `users` left join으로 `uploadedByUserName` 조회
- `KycDocumentQueryRepository`: 미리보기 응답용 업로드 사용자명 조회 메서드 추가
- `AdminKycDocumentService`: 미리보기 응답에 업로드 주체 필드 매핑
- `AdminAuditLogQueryService`: 감사 로그 응답에 before/after JSON 매핑

Credential은 현재 Entity 없이 Native SQL DTO/Repository 구조로 운영 중이다. V6 Credential 추가 컬럼은 기존 목록/상세 응답의 필수 요구 필드가 아니고, 발급/오퍼/지갑 저장 흐름과 연결된 별도 응답 설계가 필요하므로 Part 1에서는 신규 Entity를 만들지 않았다. Part 5에서 Credential 이력 API와 함께 반영한다.

## 6. AuditLog 중복 Entity 검토 결과

- 감사 로그 조회 API는 `domain.audit.domain.AuditLog`와 `AuditLogForAudit` entity name을 사용한다.
- `domain.admin.domain.AuditLog`는 관리자/공통코드/문서요건 등 기존 쓰기 흐름에서 사용된다.
- Part 1에서는 중복 Entity를 삭제하지 않고, API 조회에 직접 사용되는 `domain.audit.domain.AuditLog`만 V6 컬럼과 동기화했다.

## 7. compileJava 결과

명령:

```powershell
cd backend_admin
.\gradlew.bat clean compileJava
```

결과: 성공

## 8. 다음 Part 2에서 해야 할 작업

- 기존 API 응답 스펙을 Swagger에서 확인하고 프론트엔드 기대 필드명과 맞추기
- 신규 법인 부가정보 API 설계: representatives, agents, documents
- 문서 삭제 요청 관리 API의 상태 전이 정책 정리
- Credential 이력 API의 DTO/쿼리 설계
- Verifier/알림 템플릿 API 구현 범위 확정
