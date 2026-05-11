package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.document.domain.DocumentDeleteRequest;
import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestCreateRequest;
import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestListResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentDetailResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentListResponse;
import com.kyvc.backend.domain.document.repository.DocumentDeleteRequestRepository;
import com.kyvc.backend.domain.document.repository.UserDocumentQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

// 사용자 문서함 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class UserDocumentService {

    private static final int DEFAULT_PAGE_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_PAGE_SIZE = 100; // 최대 페이지 크기
    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE"; // 문서 유형 공통코드 그룹
    private static final List<KyvcEnums.DocumentDeleteRequestStatus> IN_PROGRESS_DELETE_STATUSES = List.of(
            KyvcEnums.DocumentDeleteRequestStatus.REQUESTED,
            KyvcEnums.DocumentDeleteRequestStatus.APPROVED
    ); // 진행 중 삭제 요청 상태 목록

    private final UserDocumentQueryRepository userDocumentQueryRepository;
    private final DocumentDeleteRequestRepository documentDeleteRequestRepository;
    private final CommonCodeProvider commonCodeProvider;
    private final AuditLogService auditLogService;

    // 사용자 문서 목록 조회
    @Transactional(readOnly = true)
    public UserDocumentListResponse getDocuments(
            Long userId, // 사용자 ID
            String documentTypeCode, // 문서 유형 코드
            String status, // 문서 상태 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        validateUserId(userId);
        String normalizedDocumentTypeCode = normalizeDocumentTypeCode(documentTypeCode); // 보정 문서 유형 코드
        String normalizedStatus = normalizeDocumentStatus(status); // 보정 문서 상태 코드
        int normalizedPage = normalizePage(page); // 보정 페이지 번호
        int normalizedSize = normalizeSize(size); // 보정 페이지 크기

        List<UserDocumentListResponse.Item> items = userDocumentQueryRepository.searchDocuments(
                userId,
                normalizedDocumentTypeCode,
                normalizedStatus,
                normalizedPage,
                normalizedSize
        );
        long totalElements = userDocumentQueryRepository.countDocuments(
                userId,
                normalizedDocumentTypeCode,
                normalizedStatus
        );
        return new UserDocumentListResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages(totalElements, normalizedSize)
        );
    }

    // 사용자 문서 상세 조회
    @Transactional(readOnly = true)
    public UserDocumentDetailResponse getDocument(
            Long userId, // 사용자 ID
            Long documentId // 문서 ID
    ) {
        validateUserId(userId);
        validateDocumentId(documentId);
        return findOwnedDocument(userId, documentId);
    }

    // 사용자 문서 삭제 요청 생성
    public UserDocumentDeleteRequestResponse createDeleteRequest(
            Long userId, // 사용자 ID
            Long documentId, // 문서 ID
            UserDocumentDeleteRequestCreateRequest request // 문서 삭제 요청 생성 요청
    ) {
        validateUserId(userId);
        validateDocumentId(documentId);
        validateDeleteRequest(request);

        UserDocumentDetailResponse document = findOwnedDocument(userId, documentId);
        if (KyvcEnums.DocumentUploadStatus.DELETED == parseDocumentUploadStatus(document.uploadStatusCode())) {
            throw new ApiException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        if (documentDeleteRequestRepository.existsByDocumentIdAndStatuses(documentId, IN_PROGRESS_DELETE_STATUSES)) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_ALREADY_EXISTS);
        }

        DocumentDeleteRequest savedRequest = documentDeleteRequestRepository.save(
                DocumentDeleteRequest.createRequested(
                        documentId,
                        userId,
                        normalizeRequired(request.reason())
                )
        );
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                "DOCUMENT_DELETE_REQUEST_CREATE",
                KyvcEnums.AuditTargetType.KYC_DOCUMENT.name(),
                documentId,
                "사용자 문서 삭제 요청 생성",
                null
        ));
        return toDeleteRequestResponse(savedRequest);
    }

    // 사용자 문서 삭제 요청 이력 조회
    @Transactional(readOnly = true)
    public UserDocumentDeleteRequestListResponse getDeleteRequests(
            Long userId, // 사용자 ID
            String status, // 삭제 요청 상태 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        validateUserId(userId);
        String normalizedStatus = normalizeDeleteRequestStatus(status); // 보정 삭제 요청 상태 코드
        int normalizedPage = normalizePage(page); // 보정 페이지 번호
        int normalizedSize = normalizeSize(size); // 보정 페이지 크기

        List<UserDocumentDeleteRequestListResponse.Item> items = userDocumentQueryRepository.searchDeleteRequests(
                userId,
                normalizedStatus,
                normalizedPage,
                normalizedSize
        );
        long totalElements = userDocumentQueryRepository.countDeleteRequests(userId, normalizedStatus); // 전체 건수
        return new UserDocumentDeleteRequestListResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages(totalElements, normalizedSize)
        );
    }

    // 사용자 소유 문서 상세 조회
    private UserDocumentDetailResponse findOwnedDocument(
            Long userId, // 사용자 ID
            Long documentId // 문서 ID
    ) {
        return userDocumentQueryRepository.findDocumentDetail(userId, documentId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    // 삭제 요청 응답 변환
    private UserDocumentDeleteRequestResponse toDeleteRequestResponse(
            DocumentDeleteRequest request // 문서 삭제 요청
    ) {
        return new UserDocumentDeleteRequestResponse(
                request.getRequestId(),
                request.getDocumentId(),
                request.getRequestedByUserId(),
                request.getRequestStatusCode().name(),
                request.getRequestReason(),
                request.getRequestedAt()
        );
    }

    // 문서 유형 코드 정규화
    private String normalizeDocumentTypeCode(
            String documentTypeCode // 원본 문서 유형 코드
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            return null;
        }
        String normalizedCode = documentTypeCode.trim().toUpperCase(Locale.ROOT); // 정규화 문서 유형 코드
        commonCodeProvider.validateEnabledCode(DOCUMENT_TYPE_GROUP, normalizedCode);
        return normalizedCode;
    }

    // 문서 상태 코드 정규화
    private String normalizeDocumentStatus(
            String status // 원본 문서 상태 코드
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseDocumentUploadStatus(status).name();
    }

    // 삭제 요청 상태 코드 정규화
    private String normalizeDeleteRequestStatus(
            String status // 원본 삭제 요청 상태 코드
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT); // 정규화 삭제 요청 상태 코드
        try {
            return KyvcEnums.DocumentDeleteRequestStatus.valueOf(normalizedStatus).name();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 문서 업로드 상태 변환
    private KyvcEnums.DocumentUploadStatus parseDocumentUploadStatus(
            String status // 문서 업로드 상태 코드
    ) {
        try {
            return KyvcEnums.DocumentUploadStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 삭제 요청 생성 요청 검증
    private void validateDeleteRequest(
            UserDocumentDeleteRequestCreateRequest request // 문서 삭제 요청 생성 요청
    ) {
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 문서 ID 검증
    private void validateDocumentId(
            Long documentId // 문서 ID
    ) {
        if (documentId == null || documentId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    // 페이지 번호 보정
    private int normalizePage(
            int page // 원본 페이지 번호
    ) {
        return Math.max(page, 0);
    }

    // 페이지 크기 보정
    private int normalizeSize(
            int size // 원본 페이지 크기
    ) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    // 전체 페이지 수 산정
    private int totalPages(
            long totalElements, // 전체 건수
            int size // 페이지 크기
    ) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
