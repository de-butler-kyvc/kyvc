# Part 4 Result: Document Delete Request Admin APIs

## 1. New APIs

- List: `GET /api/admin/backend/document-delete-requests`
- Detail: `GET /api/admin/backend/document-delete-requests/{requestId}`
- Approve: `POST /api/admin/backend/document-delete-requests/{requestId}/approve`
- Reject: `POST /api/admin/backend/document-delete-requests/{requestId}/reject`

## 2. Files

- Controller
  - `src/main/java/com/kyvc/backendadmin/domain/document/controller/AdminDocumentDeleteRequestController.java`
- Service
  - `src/main/java/com/kyvc/backendadmin/domain/document/application/AdminDocumentDeleteRequestService.java`
- Repository
  - `src/main/java/com/kyvc/backendadmin/domain/document/repository/AdminDocumentDeleteRequestQueryRepository.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/repository/AdminDocumentDeleteRequestQueryRepositoryImpl.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/repository/DocumentDeleteRequestState.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/repository/KycDocumentDeleteState.java`
- DTO
  - `src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminDocumentDeleteRequestSearchRequest.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminDocumentDeleteRequestListResponse.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminDocumentDeleteRequestDetailResponse.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminDocumentDeleteApproveRequest.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminDocumentDeleteRejectRequest.java`
  - `src/main/java/com/kyvc/backendadmin/domain/document/dto/AdminDocumentDeleteProcessResponse.java`
- Entity
  - `src/main/java/com/kyvc/backendadmin/domain/audit/domain/AuditLog.java`
- ErrorCode
  - `src/main/java/com/kyvc/backendadmin/global/exception/ErrorCode.java`
- Other
  - `src/main/java/com/kyvc/backendadmin/domain/audit/application/AuditLogWriter.java`

## 3. API Logic

- List: filters by `request_status_code`, keyword, and `requested_at`; returns `items/page/size/totalElements/totalPages`.
- Detail: returns delete request, requester, processor, corporate, and document metadata. `filePath` is returned as `null` to follow the current document metadata exposure policy.
- Approve: validates `REQUESTED`, marks the target `kyc_documents.upload_status_code` as `DELETED`, updates the request to `APPROVED`, then writes an audit log.
- Reject: validates `REQUESTED`, updates only the request to `REJECTED`, then writes an audit log.

## 4. State Policy

- `REQUESTED -> APPROVED`: used for approve because the endpoint represents an approval decision. The target KYC document is immediately marked `DELETED`.
- `REQUESTED -> REJECTED`: used for reject. The target KYC document status is unchanged.
- Already processed requests: return `400 DOCUMENT_DELETE_REQUEST_ALREADY_PROCESSED`.
- Missing delete request: returns `404 DOCUMENT_DELETE_REQUEST_NOT_FOUND`.
- Missing target document: returns `404 KYC_DOCUMENT_NOT_FOUND`.

## 5. Audit Logs

- `targetType`: `KYC_DOCUMENT`
- `targetId`: target `document_id`
- Approve `actionType`: `DOCUMENT_DELETE_REQUEST_APPROVE`
- Reject `actionType`: `DOCUMENT_DELETE_REQUEST_REJECT`
- `beforeValueJson`: request status, document status, processor fields before processing
- `afterValueJson`: request status, document status, processor fields after processing
- `processedByAdminId`: resolved from the current admin authentication context

## 6. Swagger / JavaDoc

- Added class-level `@Tag`.
- Added `@Operation` for all four APIs.
- Added `@Parameter` for path/query parameters.
- Added Swagger `@RequestBody` for approve/reject.
- Added `@ApiResponse` for success and business-error cases.
- Added `@Schema` to every DTO record field.
- Added Controller JavaDoc with `@param` and `@return`.
- Added service comments for REQUESTED guards and approve/reject state transitions.

## 7. Repository / Query

- List and detail queries use native SQL and `LEFT JOIN` for optional related rows.
- Keyword search covers corporate name, document file name, and requester email.
- Approve/reject update queries include `request_status_code = 'REQUESTED'` to reduce duplicate processing races.
- Approve updates `kyc_documents.upload_status_code` to `DELETED`.
- Detail response masks `file_path` with `null`.

## 8. Build

- `./gradlew.bat clean compileJava`: success
- `./gradlew.bat clean build`: success

## 9. Swagger/API Test

Test server: local jar on port `18083`, local DB `kyvc_back`.

- `GET /api/admin/backend/document-delete-requests?status=REQUESTED&page=0&size=10`: `200 OK`, `success=true`
- `GET /api/admin/backend/document-delete-requests/1`: `200 OK`, `success=true`
- `POST /api/admin/backend/document-delete-requests/2/approve`: `200 OK`, `success=true`
- `POST /api/admin/backend/document-delete-requests/3/reject`: `200 OK`, `success=true`
- Re-approve request `2`: `400 DOCUMENT_DELETE_REQUEST_ALREADY_PROCESSED`
- Detail request `999999`: `404 DOCUMENT_DELETE_REQUEST_NOT_FOUND`
- `/v3/api-docs`: all four paths are present

DB verification:

- Request `2`: `APPROVED`, target document `2` status `DELETED`
- Request `3`: `REJECTED`, target document `3` status remains `UPLOADED`
- Audit logs were created with `before_value_json` and `after_value_json`

## 10. Remaining Issues

- Test data was inserted into the local Docker DB only.
- Frontend should confirm whether `APPROVED` or `COMPLETED` is expected after approval; the current implementation uses `APPROVED`.
- Part 5 can proceed to Credential APIs.
