package com.kyvc.backendadmin.domain.document.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteApproveRequest;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteProcessResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRejectRequest;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestDetailResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestListResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.domain.document.repository.AdminDocumentDeleteRequestQueryRepository;
import com.kyvc.backendadmin.domain.document.repository.DocumentDeleteRequestState;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentDeleteState;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDocumentDeleteRequestService {

    private static final String STATUS_REQUESTED = "REQUESTED";

    private final AdminDocumentDeleteRequestQueryRepository queryRepository;
    private final AuditLogWriter auditLogWriter;
    private final ObjectMapper objectMapper;

    /**
     * Returns document delete requests with filters and paging.
     *
     * @param request search conditions
     * @return document delete request list response
     */
    @Transactional(readOnly = true)
    public AdminDocumentDeleteRequestListResponse getDeleteRequests(AdminDocumentDeleteRequestSearchRequest request) {
        int page = request.normalizedPage();
        int size = request.normalizedSize();
        long totalElements = queryRepository.countDeleteRequests(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AdminDocumentDeleteRequestListResponse(
                queryRepository.findDeleteRequests(request),
                page,
                size,
                totalElements,
                totalPages
        );
    }

    /**
     * Returns document delete request detail.
     *
     * @param requestId document delete request ID
     * @return document delete request detail
     */
    @Transactional(readOnly = true)
    public AdminDocumentDeleteRequestDetailResponse getDeleteRequestDetail(Long requestId) {
        return queryRepository.findDeleteRequestDetail(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_NOT_FOUND));
    }

    /**
     * Approves a document delete request and marks the target KYC document as deleted.
     *
     * @param requestId document delete request ID
     * @param request approve request
     * @return approve result
     */
    @Transactional
    public AdminDocumentDeleteProcessResponse approve(Long requestId, AdminDocumentDeleteApproveRequest request) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        DocumentDeleteRequestState beforeRequest = getRequestState(requestId);
        validateRequested(beforeRequest);
        KycDocumentDeleteState beforeDocument = getDocumentState(beforeRequest.documentId());
        LocalDateTime processedAt = LocalDateTime.now();

        String beforeValueJson = toJson(toAuditState(beforeRequest, beforeDocument));

        int documentRows = queryRepository.markKycDocumentDeleted(beforeRequest.documentId());
        if (documentRows == 0) {
            throw new ApiException(ErrorCode.KYC_DOCUMENT_NOT_FOUND);
        }

        // The update query keeps a REQUESTED guard so duplicate approve/reject races do not create conflicting states.
        int requestRows = queryRepository.approveDeleteRequest(requestId, adminId, request.processedReason(), processedAt);
        if (requestRows == 0) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_ALREADY_PROCESSED);
        }

        DocumentDeleteRequestState afterRequest = getRequestState(requestId);
        KycDocumentDeleteState afterDocument = getDocumentState(beforeRequest.documentId());
        String afterValueJson = toJson(toAuditState(afterRequest, afterDocument));
        writeAuditLog(adminId, beforeRequest.documentId(), requestId, beforeValueJson, afterValueJson, "DOCUMENT_DELETE_REQUEST_APPROVE");

        return new AdminDocumentDeleteProcessResponse(
                requestId,
                beforeRequest.documentId(),
                beforeRequest.requestStatusCode(),
                afterRequest.requestStatusCode(),
                beforeDocument.uploadStatusCode(),
                afterDocument.uploadStatusCode(),
                adminId,
                processedAt
        );
    }

    /**
     * Rejects a document delete request without changing the target KYC document.
     *
     * @param requestId document delete request ID
     * @param request reject request
     * @return reject result
     */
    @Transactional
    public AdminDocumentDeleteProcessResponse reject(Long requestId, AdminDocumentDeleteRejectRequest request) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        DocumentDeleteRequestState beforeRequest = getRequestState(requestId);
        validateRequested(beforeRequest);
        KycDocumentDeleteState documentState = getDocumentState(beforeRequest.documentId());
        LocalDateTime processedAt = LocalDateTime.now();
        String beforeValueJson = toJson(toAuditState(beforeRequest, documentState));

        // Reject changes only the request status; the original KYC document upload status remains intact.
        int requestRows = queryRepository.rejectDeleteRequest(requestId, adminId, request.processedReason(), processedAt);
        if (requestRows == 0) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_ALREADY_PROCESSED);
        }

        DocumentDeleteRequestState afterRequest = getRequestState(requestId);
        String afterValueJson = toJson(toAuditState(afterRequest, documentState));
        writeAuditLog(adminId, beforeRequest.documentId(), requestId, beforeValueJson, afterValueJson, "DOCUMENT_DELETE_REQUEST_REJECT");

        return new AdminDocumentDeleteProcessResponse(
                requestId,
                beforeRequest.documentId(),
                beforeRequest.requestStatusCode(),
                afterRequest.requestStatusCode(),
                documentState.uploadStatusCode(),
                documentState.uploadStatusCode(),
                adminId,
                processedAt
        );
    }

    private DocumentDeleteRequestState getRequestState(Long requestId) {
        return queryRepository.findDeleteRequestState(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_NOT_FOUND));
    }

    private KycDocumentDeleteState getDocumentState(Long documentId) {
        return queryRepository.findKycDocumentState(documentId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_DOCUMENT_NOT_FOUND));
    }

    private void validateRequested(DocumentDeleteRequestState state) {
        // Only REQUESTED rows are processable, which prevents repeated decisions from corrupting the audit trail.
        if (!STATUS_REQUESTED.equals(state.requestStatusCode())) {
            throw new ApiException(ErrorCode.DOCUMENT_DELETE_REQUEST_ALREADY_PROCESSED);
        }
    }

    private Map<String, Object> toAuditState(DocumentDeleteRequestState requestState, KycDocumentDeleteState documentState) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("requestId", requestState.requestId());
        values.put("documentId", requestState.documentId());
        values.put("kycId", documentState.kycId());
        values.put("requestStatusCode", requestState.requestStatusCode());
        values.put("uploadStatusCode", documentState.uploadStatusCode());
        values.put("processedByAdminId", requestState.processedByAdminId());
        values.put("processedReason", requestState.processedReason());
        values.put("processedAt", requestState.processedAt());
        return values;
    }

    private String toJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to create audit log JSON.", exception);
        }
    }

    private void writeAuditLog(
            Long adminId,
            Long documentId,
            Long requestId,
            String beforeValueJson,
            String afterValueJson,
            String actionType
    ) {
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                actionType,
                KyvcEnums.AuditTargetType.KYC_DOCUMENT,
                documentId,
                "Changed document delete request status. requestId=%d".formatted(requestId),
                beforeValueJson,
                afterValueJson
        );
    }
}
