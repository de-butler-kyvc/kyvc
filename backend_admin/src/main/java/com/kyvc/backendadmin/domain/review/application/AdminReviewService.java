package com.kyvc.backendadmin.domain.review.application;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.kyc.application.AdminKycAccessChecker;
import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.domain.review.domain.KycReviewHistory;
import com.kyvc.backendadmin.domain.review.domain.KycSupplement;
import com.kyvc.backendadmin.domain.review.dto.AdminReviewActionResponse;
import com.kyvc.backendadmin.domain.review.dto.AdminReviewApproveRequest;
import com.kyvc.backendadmin.domain.review.dto.AdminReviewRejectRequest;
import com.kyvc.backendadmin.domain.review.dto.AdminSupplementRequest;
import com.kyvc.backendadmin.domain.review.policy.KycStatusTransitionPolicy;
import com.kyvc.backendadmin.domain.review.repository.AdminReviewRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.commoncode.application.CommonCodeValidator;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * KYC 심사 액션 처리 유스케이스를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private static final String REJECT_REASON_CODE_GROUP = "REJECT_REASON";
    private static final String SUPPLEMENT_REASON_CODE_GROUP = "SUPPLEMENT_REASON";
    private static final String DOCUMENT_TYPE_CODE_GROUP = "DOCUMENT_TYPE";

    private final KycApplicationRepository kycApplicationRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final AuthTokenRepository authTokenRepository;
    private final CommonCodeValidator commonCodeValidator;
    private final KycStatusTransitionPolicy kycStatusTransitionPolicy;
    private final AdminKycAccessChecker adminKycAccessChecker;

    /**
     * KYC 수동심사 승인 액션을 처리합니다.
     *
     * <p>현재 관리자 권한 확인, MFA_SESSION 토큰 검증, 상태 전이 검증을 수행한 뒤
     * KYC 상태를 APPROVED로 변경합니다. 이후 review history 기록, audit log 기록,
     * MFA token USED 처리를 수행하고 처리 결과 DTO를 반환합니다.</p>
     *
     * @param kycId 승인할 KYC 신청 ID
     * @param request 승인 요청 DTO
     * @return 승인 처리 결과
     */
    @Transactional
    public AdminReviewActionResponse approve(Long kycId, AdminReviewApproveRequest request) {
        // 권한 확인: 수동심사 처리는 백엔드 관리자 또는 시스템 관리자만 수행할 수 있다.
        validateReviewPermission();
        adminKycAccessChecker.validateActionAccess(kycId, "KYC_MANUAL_APPROVE");
        Long adminId = SecurityUtil.getCurrentAdminId();
        // MFA_SESSION 토큰 검증: 승인 액션은 MFA 검증을 통과한 1회성 세션 토큰이 필요하다.
        AuthToken mfaToken = validateMfaSessionToken(request.mfaToken(), adminId);
        KycApplication application = getKycApplication(kycId);
        KyvcEnums.KycStatus beforeStatus = application.getKycStatusCode();
        KyvcEnums.KycStatus targetStatus = KyvcEnums.KycStatus.APPROVED;
        // 상태 전이 검증: 현재 KYC 상태에서 승인 상태로 이동 가능한지 정책으로 판단한다.
        kycStatusTransitionPolicy.validateTransition(beforeStatus, targetStatus);

        updateKycStatus(kycId, beforeStatus, targetStatus, null, null);
        // review history 기록: 승인 전후 상태와 관리자 의견을 남긴다.
        KycReviewHistory history = saveHistory(kycId, adminId, KyvcEnums.ReviewActionType.APPROVE,
                beforeStatus, targetStatus, request.comment());
        // audit log 기록: KYC 승인 주요 변경 이력을 감사로그에 남긴다.
        adminReviewRepository.saveAuditLog(AuditLog.kycApplication(
                adminId,
                kycId,
                "KYC_MANUAL_APPROVE",
                "KYC 수동심사를 승인했습니다."
        ));
        // MFA token USED 처리: 승인 액션에 사용한 MFA_SESSION 토큰은 즉시 재사용 불가 상태로 변경한다.
        mfaToken.markUsed(LocalDateTime.now());
        return toResponse(kycId, beforeStatus, targetStatus, history.getReviewHistoryId(), null);
    }

    /**
     * KYC 수동심사 반려 액션을 처리합니다.
     *
     * <p>현재 관리자 권한 확인, MFA_SESSION 토큰 검증, REJECT_REASON 공통코드 검증,
     * 상태 전이 검증을 수행한 뒤 KYC 상태를 REJECTED로 변경합니다. 이후 review history 기록,
     * audit log 기록, MFA token USED 처리를 수행하고 처리 결과 DTO를 반환합니다.</p>
     *
     * @param kycId 반려할 KYC 신청 ID
     * @param request 반려 요청 DTO
     * @return 반려 처리 결과
     */
    @Transactional
    public AdminReviewActionResponse reject(Long kycId, AdminReviewRejectRequest request) {
        // 권한 확인: 수동심사 처리는 백엔드 관리자 또는 시스템 관리자만 수행할 수 있다.
        validateReviewPermission();
        adminKycAccessChecker.validateActionAccess(kycId, "KYC_MANUAL_REJECT");
        Long adminId = SecurityUtil.getCurrentAdminId();
        // MFA_SESSION 토큰 검증: 반려 액션은 MFA 검증을 통과한 1회성 세션 토큰이 필요하다.
        AuthToken mfaToken = validateMfaSessionToken(request.mfaToken(), adminId);
        commonCodeValidator.validateEnabledCode(REJECT_REASON_CODE_GROUP, request.rejectReasonCode());
        KycApplication application = getKycApplication(kycId);
        KyvcEnums.KycStatus beforeStatus = application.getKycStatusCode();
        KyvcEnums.KycStatus targetStatus = KyvcEnums.KycStatus.REJECTED;
        // 상태 전이 검증: 현재 KYC 상태에서 반려 상태로 이동 가능한지 정책으로 판단한다.
        kycStatusTransitionPolicy.validateTransition(beforeStatus, targetStatus);

        updateKycStatus(kycId, beforeStatus, targetStatus, request.rejectReasonCode(), request.comment());
        // review history 기록: 반려 전후 상태와 반려 사유를 남긴다.
        KycReviewHistory history = saveHistory(kycId, adminId, KyvcEnums.ReviewActionType.REJECT,
                beforeStatus, targetStatus, request.comment());
        // audit log 기록: KYC 반려 주요 변경 이력을 감사로그에 남긴다.
        adminReviewRepository.saveAuditLog(AuditLog.kycApplication(
                adminId,
                kycId,
                "KYC_MANUAL_REJECT",
                "KYC 수동심사를 반려했습니다. rejectReasonCode=" + request.rejectReasonCode()
        ));
        // MFA token USED 처리: 반려 액션에 사용한 MFA_SESSION 토큰은 즉시 재사용 불가 상태로 변경한다.
        mfaToken.markUsed(LocalDateTime.now());
        return toResponse(kycId, beforeStatus, targetStatus, history.getReviewHistoryId(), null);
    }

    /**
     * KYC 보완요청 액션을 처리합니다.
     *
     * <p>현재 관리자 권한 확인, SUPPLEMENT_REASON 및 DOCUMENT_TYPE 공통코드 검증,
     * 상태 전이 검증을 수행한 뒤 KYC 상태를 NEED_SUPPLEMENT로 변경합니다. 이후 보완요청 저장,
     * review history 기록, audit log 기록을 수행하고 처리 결과 DTO를 반환합니다.</p>
     *
     * @param kycId 보완요청할 KYC 신청 ID
     * @param request 보완요청 DTO
     * @return 보완요청 처리 결과
     */
    @Transactional
    public AdminReviewActionResponse requestSupplement(Long kycId, AdminSupplementRequest request) {
        // 권한 확인: 수동심사 처리는 백엔드 관리자 또는 시스템 관리자만 수행할 수 있다.
        validateReviewPermission();
        adminKycAccessChecker.validateActionAccess(kycId, "KYC_SUPPLEMENT_REQUEST");
        Long adminId = SecurityUtil.getCurrentAdminId();
        commonCodeValidator.validateEnabledCode(SUPPLEMENT_REASON_CODE_GROUP, request.supplementReasonCode());
        request.documentTypes().forEach(documentType ->
                commonCodeValidator.validateEnabledCode(DOCUMENT_TYPE_CODE_GROUP, documentType));

        KycApplication application = getKycApplication(kycId);
        KyvcEnums.KycStatus beforeStatus = application.getKycStatusCode();
        KyvcEnums.KycStatus targetStatus = KyvcEnums.KycStatus.NEED_SUPPLEMENT;
        // 상태 전이 검증: 현재 KYC 상태에서 보완필요 상태로 이동 가능한지 정책으로 판단한다.
        kycStatusTransitionPolicy.validateTransition(beforeStatus, targetStatus);

        updateKycStatus(kycId, beforeStatus, targetStatus, request.supplementReasonCode(), request.message());
        KycSupplement supplement = KycSupplement.create(
                kycId,
                adminId,
                request.supplementReasonCode(),
                request.title(),
                request.message(),
                request.documentTypes(),
                request.dueAt()
        );
        adminReviewRepository.saveSupplement(supplement);
        // review history 기록: 보완요청 전후 상태와 요청 메시지를 남긴다.
        KycReviewHistory history = saveHistory(kycId, adminId, KyvcEnums.ReviewActionType.REQUEST_SUPPLEMENT,
                beforeStatus, targetStatus, request.message());
        // audit log 기록: 보완요청 생성 주요 변경 이력을 감사로그에 남긴다.
        adminReviewRepository.saveAuditLog(AuditLog.kycSupplement(
                adminId,
                supplement.getSupplementId(),
                "KYC_SUPPLEMENT_REQUEST",
                "KYC 보완요청을 생성했습니다. kycId=%d, supplementReasonCode=%s"
                        .formatted(kycId, request.supplementReasonCode())
        ));
        return toResponse(kycId, beforeStatus, targetStatus, history.getReviewHistoryId(), supplement.getSupplementId());
    }

    private void validateReviewPermission() {
        if (!SecurityUtil.hasRole(KyvcEnums.RoleCode.BACKEND_ADMIN.name())
                && !SecurityUtil.hasRole(KyvcEnums.RoleCode.SYSTEM_ADMIN.name())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private AuthToken validateMfaSessionToken(String rawMfaToken, Long adminId) {
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndTokenType(TokenHashUtil.sha256(rawMfaToken), KyvcEnums.TokenType.MFA_SESSION)
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_NOT_FOUND));
        if (KyvcEnums.ActorType.ADMIN != authToken.getActorType() || !adminId.equals(authToken.getActorId())) {
            throw new ApiException(ErrorCode.MFA_NOT_FOUND);
        }
        if (KyvcEnums.TokenStatus.USED == authToken.getStatus()) {
            throw new ApiException(ErrorCode.MFA_ALREADY_USED);
        }
        if (!authToken.isActive()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        if (authToken.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_EXPIRED);
        }
        return authToken;
    }

    private KycApplication getKycApplication(Long kycId) {
        return kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    private void updateKycStatus(
            Long kycId,
            KyvcEnums.KycStatus beforeStatus,
            KyvcEnums.KycStatus targetStatus,
            String reasonCode,
            String reasonMessage
    ) {
        int updatedRows = adminReviewRepository.updateKycStatus(kycId, beforeStatus, targetStatus, reasonCode, reasonMessage);
        if (updatedRows == 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "KYC 상태 변경 조건을 만족하지 않습니다.");
        }
    }

    private KycReviewHistory saveHistory(
            Long kycId,
            Long adminId,
            KyvcEnums.ReviewActionType actionType,
            KyvcEnums.KycStatus beforeStatus,
            KyvcEnums.KycStatus targetStatus,
            String comment
    ) {
        return adminReviewRepository.saveReviewHistory(KycReviewHistory.create(
                kycId,
                adminId,
                actionType,
                beforeStatus,
                targetStatus,
                comment
        ));
    }

    private AdminReviewActionResponse toResponse(
            Long kycId,
            KyvcEnums.KycStatus beforeStatus,
            KyvcEnums.KycStatus targetStatus,
            Long historyId,
            Long supplementId
    ) {
        return new AdminReviewActionResponse(
                kycId,
                beforeStatus.name(),
                targetStatus.name(),
                historyId,
                supplementId,
                LocalDateTime.now()
        );
    }
}
