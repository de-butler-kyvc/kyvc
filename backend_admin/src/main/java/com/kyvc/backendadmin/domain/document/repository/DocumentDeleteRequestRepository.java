package com.kyvc.backendadmin.domain.document.repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 원본서류 삭제 요청 단건 조회와 상태 변경 Repository입니다.
 */
public interface DocumentDeleteRequestRepository {

    /**
     * 삭제 요청 ID로 삭제 요청을 조회합니다.
     *
     * @param requestId 삭제 요청 ID
     * @return 삭제 요청 행
     */
    Optional<Row> findById(Long requestId);

    /**
     * 삭제 요청을 승인 상태로 변경합니다.
     *
     * @param requestId 삭제 요청 ID
     * @param adminId 처리 관리자 ID
     * @param processedReason 처리 사유
     * @param processedAt 처리 일시
     * @return 수정된 행 수
     */
    int approve(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt);

    /**
     * 삭제 요청을 반려 상태로 변경합니다.
     *
     * @param requestId 삭제 요청 ID
     * @param adminId 처리 관리자 ID
     * @param processedReason 처리 사유
     * @param processedAt 처리 일시
     * @return 수정된 행 수
     */
    int reject(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt);

    /**
     * document_delete_requests 단건 행입니다.
     *
     * @param requestId 삭제 요청 ID
     * @param documentId 문서 ID
     * @param requestedByUserId 요청 사용자 ID
     * @param status 삭제 요청 상태
     * @param requestReason 요청 사유
     * @param processedByAdminId 처리 관리자 ID
     * @param processedReason 처리 사유
     * @param requestedAt 요청 일시
     * @param processedAt 처리 일시
     */
    record Row(
            Long requestId,
            Long documentId,
            Long requestedByUserId,
            String status,
            String requestReason,
            Long processedByAdminId,
            String processedReason,
            LocalDateTime requestedAt,
            LocalDateTime processedAt
    ) {
    }
}
