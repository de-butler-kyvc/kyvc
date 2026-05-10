package com.kyvc.backendadmin.domain.document.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.document.domain.KycDocument;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestActionResponse;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestApproveRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestListResponse;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestRejectRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSummaryResponse;
import com.kyvc.backendadmin.domain.document.repository.DocumentDeleteRequestQueryRepository;
import com.kyvc.backendadmin.domain.document.repository.DocumentDeleteRequestRepository;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 원본서류 삭제 요청 조회와 승인/반려 처리를 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class DocumentDeleteRequestService {

    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_DELETED = "DELETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            STATUS_REQUESTED,
            STATUS_PENDING,
            STATUS_APPROVED,
            STATUS_REJECTED,
            STATUS_DELETED,
            STATUS_CANCELLED
    );
    private static final Set<String> PROCESSABLE_STATUSES = Set.of(STATUS_REQUESTED, STATUS_PENDING);

    private final DocumentDeleteRequestRepository documentDeleteRequestRepository;
    private final DocumentDeleteRequestQueryRepository documentDeleteRequestQueryRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final AuthTokenRepository authTokenRepository;
    private final AuditLogWriter auditLogWriter;

    /**
     * 원본서류 삭제 요청 목록을 검색합니다.
     *
     * @param request 검색 조건
     * @return 삭제 요청 목록 응답
     */
    @Transactional(readOnly = true)
    public DocumentDeleteRequestListResponse search(DocumentDeleteRequestSearchRequest request) {
        validateStatus(request.status());
        validatePeriod(request.startDate(), request.endDate());
        List<DocumentDeleteRequestSummaryResponse> items = documentDeleteRequestQueryRepository.search(request);
        long totalElements = documentDeleteRequestQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new DocumentDeleteRequestListResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /**
     * 원본서류 삭제 요청을 승인하고 문서를 논리 삭제 상태로 변경합니다.
     *
     * @param requestId 삭제 요청 ID
     * @param request 승인 요청 정보
     * @return 처리 결과
     */
    @Transactional
    public DocumentDeleteRequestActionResponse approve(Long requestId, DocumentDeleteRequestApproveRequest request) {
        if (request == null || !StringUtils.hasText(request.mfaToken())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        DocumentDeleteRequestRepository.Row deleteRequest = findRequestedDeleteRequest(requestId);
        KycDocument document = kycDocumentRepository.findById(deleteRequest.documentId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_DOCUMENT_NOT_FOUND));
        AuthToken mfaToken = validateMfaToken(request.mfaToken());

        LocalDateTime processedAt = LocalDateTime.now();
        Long adminId = SecurityUtil.getCurrentAdminId();
        String comment = StringUtils.hasText(request.comment()) ? request.comment() : "원본서류 삭제 승인";

        int documentUpdated = kycDocumentRepository.updateUploadStatus(
                document.getDocumentId(),
                KyvcEnums.DocumentUploadStatus.DELETED
        );
        if (documentUpdated != 1) {
            throw new ApiException(ErrorCode.KYC_DOCUMENT_NOT_FOUND);
        }

        int requestUpdated = documentDeleteRequestRepository.approve(requestId, adminId, comment, processedAt);
        if (requestUpdated != 1) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_NOT_FOUND);
        }
        mfaToken.markUsed(LocalDateTime.now());

        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                "DOCUMENT_DELETE_REQUEST_APPROVED",
                KyvcEnums.AuditTargetType.KYC_DOCUMENT,
                document.getDocumentId(),
                "원본서류 삭제 요청을 승인했습니다. requestId=%d, documentId=%d".formatted(requestId, document.getDocumentId()),
                deleteRequest.status(),
                STATUS_APPROVED
        );

        return new DocumentDeleteRequestActionResponse(
                requestId,
                document.getDocumentId(),
                STATUS_APPROVED,
                KyvcEnums.DocumentUploadStatus.DELETED.name(),
                processedAt.toString()
        );
    }

    /**
     * 원본서류 삭제 요청을 반려합니다.
     *
     * @param requestId 삭제 요청 ID
     * @param request 반려 요청 정보
     * @return 처리 결과
     */
    @Transactional
    public DocumentDeleteRequestActionResponse reject(Long requestId, DocumentDeleteRequestRejectRequest request) {
        if (request == null || !StringUtils.hasText(request.mfaToken()) || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        DocumentDeleteRequestRepository.Row deleteRequest = findRequestedDeleteRequest(requestId);
        KycDocument document = kycDocumentRepository.findById(deleteRequest.documentId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_DOCUMENT_NOT_FOUND));
        AuthToken mfaToken = validateMfaToken(request.mfaToken());

        LocalDateTime processedAt = LocalDateTime.now();
        Long adminId = SecurityUtil.getCurrentAdminId();
        String processedReason = buildRejectReason(request.reason(), request.comment());
        int requestUpdated = documentDeleteRequestRepository.reject(requestId, adminId, processedReason, processedAt);
        if (requestUpdated != 1) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_NOT_FOUND);
        }
        mfaToken.markUsed(LocalDateTime.now());

        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                "DOCUMENT_DELETE_REQUEST_REJECTED",
                KyvcEnums.AuditTargetType.KYC_DOCUMENT,
                document.getDocumentId(),
                "원본서류 삭제 요청을 반려했습니다. requestId=%d, documentId=%d".formatted(requestId, document.getDocumentId()),
                deleteRequest.status(),
                STATUS_REJECTED
        );

        return new DocumentDeleteRequestActionResponse(
                requestId,
                document.getDocumentId(),
                STATUS_REJECTED,
                document.getUploadStatusCode().name(),
                processedAt.toString()
        );
    }

    private DocumentDeleteRequestRepository.Row findRequestedDeleteRequest(Long requestId) {
        DocumentDeleteRequestRepository.Row deleteRequest = documentDeleteRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_NOT_FOUND));
        if (!PROCESSABLE_STATUSES.contains(deleteRequest.status())) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_ALREADY_PROCESSED);
        }
        return deleteRequest;
    }

    private void validateStatus(String status) {
        if (StringUtils.hasText(status) && !ALLOWED_STATUSES.contains(status)) {
            throw new ApiException(ErrorCode.INVALID_DOCUMENT_DELETE_REQUEST_STATUS);
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private AuthToken validateMfaToken(String rawMfaToken) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndTokenType(TokenHashUtil.sha256(rawMfaToken), KyvcEnums.TokenType.MFA_SESSION)
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_TOKEN_INVALID));
        if (KyvcEnums.ActorType.ADMIN != authToken.getActorType()
                || !adminId.equals(authToken.getActorId())
                || !authToken.isActive()
                || authToken.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        return authToken;
    }

    private String buildRejectReason(String rejectReason, String comment) {
        if (!StringUtils.hasText(comment)) {
            return rejectReason;
        }
        return "%s | comment=%s".formatted(rejectReason, comment);
    }
}
