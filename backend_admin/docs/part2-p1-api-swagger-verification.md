# Part 2: Existing P1 API / Swagger Verification Result

작성일: 2026-05-09
브랜치: `feature/backend-admin-global`

## 1. Swagger 테스트 결과 요약

검증 방식:

- `backend` 앱을 local DB에 잠시 기동해 Flyway V1~V11 적용 확인
- `backend_admin` bootJar를 `server.port=18080`, `spring.flyway.enabled=false`, DB `localhost:5433/kyvc_back`으로 기동
- Swagger UI와 동일한 HTTP endpoint를 직접 호출하고 `/v3/api-docs` 노출 여부 확인
- 초기 admin seed의 password hash가 `{change-me-bcrypt-hash}` placeholder라 로그인 성공은 불가하여, 보호 API는 테스트용 JWT를 생성해 호출

성공 API:

- `GET /health`: 200
- `GET /api/admin/auth/session`: 200, CommonResponse
- `GET /api/admin/backend/users`: 200, CommonResponse
- `GET /api/admin/backend/users/{userId}`: 200, CommonResponse
- `PATCH /api/admin/backend/users/{userId}/status`: 200, CommonResponse
- `GET /api/admin/backend/corporates/{corporateId}`: 200, CommonResponse
- `GET /api/admin/backend/kyc/applications`: 200, CommonResponse
- `GET /api/admin/backend/kyc/applications/{kycId}`: 200, CommonResponse
- `GET /api/admin/backend/kyc/applications/{kycId}/corporate`: 200, CommonResponse
- `GET /api/admin/backend/kyc/applications/{kycId}/documents`: 200, CommonResponse
- `GET /api/admin/backend/kyc/applications/{kycId}/documents/{documentId}/preview`: 200, CommonResponse
- `GET /api/admin/backend/audit-logs`: 200, CommonResponse
- `GET /api/admin/backend/audit-logs/{auditId}`: 200, CommonResponse
- `GET /v3/api-docs`: 200

실패 API:

- `POST /api/admin/auth/login`: 401 `AUTH_LOGIN_FAILED`
- `POST /api/admin/auth/mfa/challenge`: 401
- `POST /api/admin/auth/mfa/verify`: 401

500 발생 API:

- 없음

400/404이지만 정상 비즈니스 예외인 API:

- `GET /api/admin/backend/users/99999`: 404 `USER_NOT_FOUND`, CommonResponse
- `GET /api/admin/backend/kyc/applications/99999`: 404 `KYC_NOT_FOUND`, CommonResponse
- `GET /api/admin/backend/audit-logs/99999`: 404 `AUDIT_LOG_NOT_FOUND`, CommonResponse

## 2. API별 신규 필드 응답 확인

- users 목록: `userName`, `phone`, `notificationEnabledYn`, `mfaEnabledYn`, `mfaTypeCode`, `lastPasswordChangedAt`, `onboardingCorporateName` 확인
- users 상세: `data.user`에 `userName`, `phone`, `notificationEnabledYn`, `mfaEnabledYn`, `mfaTypeCode`, `lastPasswordChangedAt`, `onboardingCorporateName` 확인
- corporates 상세: `corporatePhone`, `corporateTypeCode`, `establishedDate`, `website` 확인. `representativeName = null` 샘플도 200 응답
- KYC 목록: `applicationChannelCode`, `financeInstitutionCode`, `financeBranchCode`, `financeStaffUserId`, `financeCustomerNo`, `visitedAt` 확인
- KYC 상세: `applicationChannelCode`, `financeInstitutionCode`, `financeBranchCode`, `financeStaffUserId`, `financeCustomerNo`, `visitedAt` 확인
- KYC 법인정보: KYC 신규 필드와 `corporatePhone`, `corporateTypeCode`, `establishedDate`, `website` 확인
- KYC 문서 목록: `uploadedByTypeCode`, `uploadedByUserId`, `uploadedByUserName` 확인
- KYC 문서 미리보기: `uploadedByTypeCode`, `uploadedByUserId`, `uploadedByUserName` 확인
- 감사 로그 목록: `beforeValueJson`, `afterValueJson` 확인
- 감사 로그 상세: `beforeValueJson`, `afterValueJson` 확인

## 3. 수정한 파일 목록

Controller:

- 없음

Service:

- 없음

Repository:

- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/repository/KycApplicationQueryRepositoryImpl.java`

DTO:

- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/kyc/dto/AdminKycApplicationCorporateResponse.java`

Entity:

- 없음

문서:

- `backend_admin/docs/part2-p1-api-swagger-verification.md`

## 4. Swagger 주석 보강 내용

- `@Tag`: 기존 Controller에 존재 확인
- `@Operation`: 기존 P1 Controller 메서드에 존재 확인
- `@Parameter`: path/query parameter 설명 존재 확인
- `@Schema`: Part 1/Part 2 추가 DTO 필드에 설명 존재 확인
- `@ApiResponse`: KYC, 문서, 감사 로그 조회의 404/403 설명 존재 확인. Corporate 쪽은 `@Operation` 중심으로 문서화되어 있어 Part 3 이후 필요 시 `@ApiResponse` 보강 가능

## 5. JavaDoc / 일반 주석 추가 내용

- Controller JavaDoc: 기존 P1 Controller 메서드에 `@param`, `@return` 존재 확인
- DTO 필드 주석: record field의 `@Schema` 설명 존재 확인
- Service 로직 주석: 기존 주요 조회/상태변경 로직 설명 주석 존재 확인

## 6. compileJava 결과

명령:

```powershell
cd backend_admin
.\gradlew.bat clean compileJava
```

성공 여부: 성공

## 7. clean build 결과

명령:

```powershell
cd backend_admin
.\gradlew.bat clean build
```

성공 여부: 성공

## 8. 남은 이슈

- DB 데이터 부족: 기본 migration seed만으로는 P1 API 응답 필드 검증용 법인/KYC/문서 데이터가 없어 Part 2 샘플 데이터를 로컬 DB에 추가해 확인했다.
- 테스트 불가 API: 로그인/MFA 성공 플로우는 seed admin password hash가 placeholder라 성공 케이스 검증 불가. 실패 응답은 401로 확인했다.
- 프론트 기대 필드명 확인 필요: `corporateType`과 `corporateTypeCode`가 KYC 법인정보 응답에 함께 존재한다. 프론트에서 어느 필드를 기준으로 사용할지 확인 필요.
- Part 3에서 이어서 처리할 내용:
  - 신규 법인 부가정보 API 구현
  - 문서 삭제 요청 관리 API 설계/구현
  - Credential 이력 API 설계
  - Verifier/Notification API 구현 범위 확정

