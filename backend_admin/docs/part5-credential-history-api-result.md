# Part 5 결과 보고: Credential 요청 / 상태 이력 API

## 1. 신규 구현 API 목록

- Credential 요청 이력 조회: `GET /api/admin/backend/credentials/{credentialId}/requests`
- Credential 상태 이력 조회: `GET /api/admin/backend/credentials/{credentialId}/status-histories`

## 2. 생성/수정한 파일 목록

- Controller
  - `src/main/java/com/kyvc/backendadmin/domain/credential/controller/AdminCredentialController.java`
- Service
  - `src/main/java/com/kyvc/backendadmin/domain/credential/application/AdminCredentialQueryService.java`
- Repository
  - `src/main/java/com/kyvc/backendadmin/domain/credential/repository/CredentialQueryRepository.java`
  - `src/main/java/com/kyvc/backendadmin/domain/credential/repository/CredentialQueryRepositoryImpl.java`
- DTO
  - `src/main/java/com/kyvc/backendadmin/domain/credential/dto/AdminCredentialRequestResponse.java`
  - `src/main/java/com/kyvc/backendadmin/domain/credential/dto/AdminCredentialStatusHistoryResponse.java`
  - `src/main/java/com/kyvc/backendadmin/domain/credential/dto/AdminCredentialDetailResponse.java`
- Entity
  - 없음
- ErrorCode
  - 신규 추가 없음. 기존 `CREDENTIAL_NOT_FOUND` 재사용

## 3. API별 처리 로직

- 요청 이력 조회
  - `credentialId` 존재 여부를 먼저 확인한다.
  - 존재하지 않으면 `CREDENTIAL_NOT_FOUND`를 반환한다.
  - `credential_requests`를 `credential_id` 기준으로 조회한다.
  - `core_request_id` 기준으로 `core_requests`를 `LEFT JOIN`한다.
  - `requested_at desc`, `credential_request_id desc` 순으로 정렬한다.

- 상태 이력 조회
  - `credentialId` 존재 여부를 먼저 확인한다.
  - 존재하지 않으면 `CREDENTIAL_NOT_FOUND`를 반환한다.
  - `credential_status_histories`를 `credential_id` 기준으로 조회한다.
  - `changed_at desc`, `history_id desc` 순으로 정렬한다.

## 4. Credential 존재 여부 검증 방식

- 사용 Repository: `CredentialQueryRepository.existsById(Long credentialId)`
- 사용 ErrorCode: `CREDENTIAL_NOT_FOUND`
- 없는 `credentialId` 처리: 404 정상 비즈니스 예외
- 이력 데이터가 없는 경우: 200 + 빈 배열

## 5. Repository / Query 구현 내용

- 요청 이력 조회 쿼리
  - `credential_requests` 기준 조회
  - `core_requests`는 `LEFT JOIN`
  - `core_request_id`가 null이거나 row가 없어도 요청 이력은 응답

- 상태 이력 조회 쿼리
  - `credential_status_histories` 기준 조회
  - `before_status_code`, `changed_by_id` null 허용

- 정렬 기준
  - 요청 이력: `requested_at desc`, `credential_request_id desc`
  - 상태 이력: `changed_at desc`, `history_id desc`

## 6. 기존 Credential 상세 DTO V6 컬럼 검토 결과

- `offerTokenHash`: 미반영. 보안상 토큰 해시는 응답에서 제외
- `offerExpiresAt`: 반영
- `offerUsedYn`: 반영
- `holderDid`: 반영
- `holderXrplAddress`: 반영
- `walletSavedAt`: 반영
- 반영 방식: 기존 상세 DTO와 Native SQL select에 필드 추가
- TODO: 프론트 상세 화면에서 `walletSavedYn`, `credentialStatusId`까지 필요한지 확인 필요

## 7. Swagger 주석 작성 내용

- `@Tag`: 기존 Credential Controller 태그를 한국어 설명으로 정리
- `@Operation`: 신규 API 2개 summary/description 한국어 작성
- `@Parameter`: `credentialId` 설명 작성
- `@Schema`: 신규 DTO 필드 전부 한국어 설명 작성
- `@ApiResponse`: 200, 404 응답 설명 작성

## 8. JavaDoc / 일반 주석 작성 내용

- Controller JavaDoc: 신규 API와 기존 Credential API 설명을 한국어로 정리
- DTO 필드 설명: 신규 DTO 전체와 상세 DTO 추가 필드 한국어 설명 작성
- Service 로직 주석: 이력이 없을 때 빈 배열을 반환하기 위한 Credential 존재 검증 주석 작성
- Repository 주석: 민감정보 제외 기준 주석 작성

## 9. compileJava 결과

- 성공 여부: 성공
- 명령: `./gradlew.bat clean compileJava`

## 10. clean build 결과

- 성공 여부: 성공
- 명령: `./gradlew.bat clean build`

## 11. Swagger 테스트 결과

- 성공 API
  - `GET /api/admin/backend/credentials/1/requests`: 200, `data` 2건
  - `GET /api/admin/backend/credentials/1/status-histories`: 200, `data` 2건
- 빈 배열 응답 확인
  - `GET /api/admin/backend/credentials/2/requests`: 200, 빈 배열
  - `GET /api/admin/backend/credentials/2/status-histories`: 200, 빈 배열
- 404 정상 비즈니스 예외
  - `GET /api/admin/backend/credentials/999999/requests`: 404 `CREDENTIAL_NOT_FOUND`
  - `GET /api/admin/backend/credentials/999999/status-histories`: 404 `CREDENTIAL_NOT_FOUND`
- 500 발생 여부: 없음
- `/v3/api-docs`: 신규 API 2개 path 노출 확인

## 12. 테스트 데이터

- `credentialId=1`: 요청 이력 2건, 상태 이력 2건 추가
- `credentialId=2`: 이력이 없는 빈 배열 테스트용 Credential 추가
- `core_request_id` null 케이스와 `core_requests` 조인 케이스 모두 포함

## 13. 남은 이슈

- 테스트 데이터는 로컬 Docker DB에만 삽입했다.
- 프론트에서 Credential 상세 응답에 `walletSavedYn`, `credentialStatusId`가 필요한지 확인 필요
- Part 6에서는 Verifier API 구현으로 이어갈 수 있다.
