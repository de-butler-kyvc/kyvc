# Part 3: Corporate Additional Info API Result

작성일: 2026-05-09
브랜치: `feature/backend-admin-global`

## 1. 신규 구현 API 목록

- 대표자 조회: `GET /api/admin/backend/corporates/{corporateId}/representatives`
- 대리인 조회: `GET /api/admin/backend/corporates/{corporateId}/agents`
- 법인문서 조회: `GET /api/admin/backend/corporates/{corporateId}/documents`

## 2. 생성/수정한 파일 목록

Controller:

- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/controller/AdminCorporateController.java`

Service:

- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/application/AdminCorporateService.java`

Repository:

- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/repository/AdminCorporateAdditionalInfoQueryRepository.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/repository/AdminCorporateAdditionalInfoQueryRepositoryImpl.java`

DTO:

- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/dto/AdminCorporateRepresentativeResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/dto/AdminCorporateAgentResponse.java`
- `backend_admin/src/main/java/com/kyvc/backendadmin/domain/corporate/dto/AdminCorporateDocumentResponse.java`

Entity:

- 없음

기타:

- `backend_admin/docs/part3-corporate-additional-info-api-result.md`

## 3. API별 처리 로직

- 대표자 조회: 법인 존재 확인 후 `corporate_representatives`를 `corporate_id`로 조회하고 `identity_document_id` 기준 `corporate_documents`를 LEFT JOIN한다.
- 대리인 조회: 법인 존재 확인 후 `corporate_agents`를 `corporate_id`로 조회하고 신분증/위임장 문서를 각각 다른 별칭으로 LEFT JOIN한다.
- 법인문서 조회: 법인 존재 확인 후 `corporate_documents`를 조회하고 `common_codes`에서 문서 유형명, `users`에서 업로드 사용자명을 LEFT JOIN한다.

## 4. Swagger 주석 작성 내용

- `@Tag`: 기존 `Corporate Admin` Controller tag 아래 신규 API 노출
- `@Operation`: 대표자/대리인/법인문서 조회 summary, description 작성
- `@Parameter`: `corporateId` path parameter 설명 작성
- `@Schema`: 신규 DTO record 필드마다 설명 작성
- `@ApiResponse`: corporateId 미존재 404 설명 작성

## 5. JavaDoc / 일반 주석 작성 내용

- Controller JavaDoc: 신규 3개 메서드에 `@param`, `@return` 작성
- DTO 필드 설명: 신규 record field 전체에 `@Schema` 작성
- Service 로직 주석: 부가정보 조회 전 법인 존재 확인 의도 주석 작성

## 6. Repository / Query 구현 내용

- 대표자 조회 쿼리:
  - `corporate_representatives representative`
  - `left join corporate_documents identity_document`
  - `identity_document.file_name as identity_document_name`
- 대리인 조회 쿼리:
  - `corporate_agents agent`
  - `left join corporate_documents identity_document`
  - `left join corporate_documents delegation_document`
- 법인문서 조회 쿼리:
  - `corporate_documents document`
  - `left join common_code_groups/common_codes`로 `documentTypeName` 조회
  - `left join users uploader`로 `uploadedByUserName` 조회
  - 기존 KYC 문서 API 보안 정책과 맞춰 `filePath`는 DTO 필드는 유지하되 `null`로 응답
- LEFT JOIN 처리:
  - identity/delegation document, document type code, uploaded user가 없어도 null로 매핑되어 500이 발생하지 않도록 처리

## 7. compileJava 결과

성공 여부: 성공

명령:

```powershell
cd backend_admin
.\gradlew.bat clean compileJava
```

## 8. clean build 결과

성공 여부: 성공

명령:

```powershell
cd backend_admin
.\gradlew.bat clean build
```

## 9. Swagger 테스트 결과

성공 API:

- `GET /api/admin/backend/corporates/1/representatives`: 200, `success=true`, `code=OK`, data 배열, count 1
- `GET /api/admin/backend/corporates/1/agents`: 200, `success=true`, `code=OK`, data 배열, count 1
- `GET /api/admin/backend/corporates/1/documents`: 200, `success=true`, `code=OK`, data 배열, count 2
- `/v3/api-docs`: 신규 3개 path 노출 확인

빈 목록 확인:

- `GET /api/admin/backend/corporates/2/representatives`: 200, 빈 배열
- `GET /api/admin/backend/corporates/2/agents`: 200, 빈 배열
- `GET /api/admin/backend/corporates/2/documents`: 200, 빈 배열

404 정상 비즈니스 예외:

- `GET /api/admin/backend/corporates/99999/representatives`: 404 `CORPORATE_NOT_FOUND`

500 발생 여부:

- 없음

## 10. 남은 이슈

- 테스트 데이터 부족: migration 기본 데이터에는 corporate additional info가 없어 로컬 검증용 대표자/대리인/법인문서 데이터를 추가해 확인했다.
- 프론트 연동 시 확인 필요 필드: 법인문서 `filePath`는 보안 정책상 null로 응답한다. 프론트가 원본 경로를 직접 기대하지 않도록 확인 필요.
- Part 4에서 이어서 처리할 내용: 문서 삭제 요청 관리 API 구현

