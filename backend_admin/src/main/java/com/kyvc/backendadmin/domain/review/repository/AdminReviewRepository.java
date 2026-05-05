package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.review.domain.KycReviewHistory;
import com.kyvc.backendadmin.domain.review.domain.KycSupplement;
import com.kyvc.backendadmin.global.util.KyvcEnums;

public interface AdminReviewRepository {

    /**
     * review history 저장을 위해 kyc_review_histories 테이블에 심사 이력을 저장합니다.
     *
     * @param history 저장할 심사 이력 엔티티
     * @return 저장된 심사 이력 엔티티
     */
    KycReviewHistory saveReviewHistory(KycReviewHistory history);

    /**
     * supplement 저장을 위해 kyc_supplements 테이블에 보완요청을 저장합니다.
     *
     * @param supplement 저장할 보완요청 엔티티
     * @return 저장된 보완요청 엔티티
     */
    KycSupplement saveSupplement(KycSupplement supplement);

    /**
     * KYC 상태 변경 조건으로 현재 상태가 expectedStatus인 경우에만 목표 상태로 변경합니다.
     *
     * @param kycId 변경할 KYC 신청 ID
     * @param expectedStatus 변경 전 기대 KYC 상태
     * @param targetStatus 변경할 KYC 상태
     * @param reasonCode 반려 또는 보완 공통코드
     * @param reasonMessage 반려 또는 보완 상세 사유
     * @return 변경된 행 수
     */
    int updateKycStatus(
            Long kycId,
            KyvcEnums.KycStatus expectedStatus,
            KyvcEnums.KycStatus targetStatus,
            String reasonCode,
            String reasonMessage
    );

    /**
     * audit log 기록을 위해 audit_logs 테이블에 감사로그를 저장합니다.
     *
     * @param auditLog 저장할 감사로그 엔티티
     * @return 저장된 감사로그 엔티티
     */
    AuditLog saveAuditLog(AuditLog auditLog);
}
