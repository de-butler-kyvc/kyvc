package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestDetailResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestListResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestSearchRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminDocumentDeleteRequestQueryRepository {

    List<AdminDocumentDeleteRequestListResponse.Item> findDeleteRequests(AdminDocumentDeleteRequestSearchRequest request);

    long countDeleteRequests(AdminDocumentDeleteRequestSearchRequest request);

    Optional<AdminDocumentDeleteRequestDetailResponse> findDeleteRequestDetail(Long requestId);

    Optional<DocumentDeleteRequestState> findDeleteRequestState(Long requestId);

    Optional<KycDocumentDeleteState> findKycDocumentState(Long documentId);

    int markKycDocumentDeleted(Long documentId);

    int approveDeleteRequest(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt);

    int rejectDeleteRequest(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt);
}
