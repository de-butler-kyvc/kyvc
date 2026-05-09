package com.kyvc.backendadmin.domain.corporate.application;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateAgentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDocumentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateRepresentativeResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserListResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserSearchRequest;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserStatusUpdateRequest;
import com.kyvc.backendadmin.domain.corporate.repository.AdminCorporateAdditionalInfoQueryRepository;
import com.kyvc.backendadmin.domain.corporate.repository.CorporateQueryRepository;
import com.kyvc.backendadmin.domain.corporate.repository.CorporateRepository;
import com.kyvc.backendadmin.domain.user.domain.User;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 법인 사용자 목록/상세/상태변경/법인상세 조회 유스케이스를 담당합니다.
 *
 * <p>관리자 법인 사용자 관리 화면에서 필요한 users, corporates, kyc_applications
 * 조인 조회와 사용자 상태 변경, refresh token 폐기, 감사로그 기록을 처리합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminCorporateService {

    private final CorporateRepository corporateRepository;
    private final CorporateQueryRepository corporateQueryRepository;
    private final AdminCorporateAdditionalInfoQueryRepository additionalInfoQueryRepository;
    private final AuthTokenRepository authTokenRepository;

    /**
     * 법인 사용자 목록을 검색합니다.
     *
     * <p>사용자 상태와 최근 KYC 상태 enum 값을 검증한 뒤 users, corporates,
     * kyc_applications 최신 1건을 조인하여 조회합니다. 읽기 전용 조회이므로
     * refresh token 폐기와 감사로그 기록은 수행하지 않습니다.</p>
     *
     * @param request 법인 사용자 검색 조건
     * @return 법인 사용자 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminCorporateUserListResponse searchUsers(AdminCorporateUserSearchRequest request) {
        validateSearchEnums(request);
        List<AdminCorporateUserListResponse.Item> items = corporateQueryRepository.searchUsers(request);
        long totalElements = corporateQueryRepository.countUsers(request);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / request.size());
        return new AdminCorporateUserListResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages
        );
    }

    /**
     * 법인 사용자 상세를 조회합니다.
     *
     * <p>userId 기준으로 법인 사용자 존재 여부를 검증하고 users, corporates,
     * kyc_applications 최신 1건을 조인하여 상세를 조회합니다. 읽기 전용 조회이므로
     * refresh token 폐기와 감사로그 기록은 수행하지 않습니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 법인 사용자 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminCorporateUserDetailResponse getUserDetail(Long userId) {
        return corporateQueryRepository.findUserDetail(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 법인 사용자 계정 상태를 변경합니다.
     *
     * <p>userId로 사용자를 조회하고 CORPORATE_USER 유형인지 확인한 뒤 요청 상태를
     * UserStatus enum으로 검증합니다. 사용자를 LOCKED, INACTIVE, WITHDRAWN 상태로
     * 변경하는 경우 해당 사용자의 활성 refresh token을 폐기하며, 모든 상태 변경은
     * audit_logs에 감사로그로 기록합니다.</p>
     *
     * @param userId 상태를 변경할 사용자 ID
     * @param request 상태 변경 요청
     * @return 변경 후 법인 사용자 상세 응답
     */
    @Transactional
    public AdminCorporateUserDetailResponse updateUserStatus(
            Long userId,
            AdminCorporateUserStatusUpdateRequest request
    ) {
        User user = corporateRepository.findUserById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateCorporateUser(user);

        KyvcEnums.UserStatus beforeStatus = user.getUserStatusCode();
        KyvcEnums.UserStatus afterStatus = parseUserStatus(request.status());

        // 사용자 상태 변경: 검증된 enum 값만 users.user_status_code에 반영한다.
        user.changeStatus(afterStatus);

        // refresh token 폐기: 잠금/비활성/탈퇴 상태에서는 기존 세션을 사용할 수 없도록 한다.
        if (shouldRevokeRefreshTokens(afterStatus)) {
            authTokenRepository.revokeActiveTokens(
                    KyvcEnums.ActorType.USER,
                    user.getUserId(),
                    KyvcEnums.TokenType.REFRESH
            );
        }

        // 감사로그 기록: 누가 어떤 사용자 상태를 어떻게 바꾸었는지 audit_logs에 남긴다.
        corporateRepository.saveAuditLog(AuditLog.user(
                SecurityUtil.getCurrentAdminId(),
                user.getUserId(),
                "CORPORATE_USER_STATUS_UPDATE",
                buildStatusChangeDescription(beforeStatus, afterStatus, request.reason())
        ));

        return getUserDetail(userId);
    }

    /**
     * 법인 상세 정보를 조회합니다.
     *
     * <p>corporateId 기준으로 법인 존재 여부를 검증하고 users, corporates,
     * kyc_applications 최신 1건을 조인하여 상세를 조회합니다. 읽기 전용 조회이므로
     * refresh token 폐기와 감사로그 기록은 수행하지 않습니다.</p>
     *
     * @param corporateId 조회할 법인 ID
     * @return 법인 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminCorporateDetailResponse getCorporateDetail(Long corporateId) {
        return corporateQueryRepository.findCorporateDetail(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
    }

    /**
     * 법인 대표자 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인 대표자 목록
     */
    @Transactional(readOnly = true)
    public List<AdminCorporateRepresentativeResponse> getCorporateRepresentatives(Long corporateId) {
        validateCorporateExists(corporateId);
        return additionalInfoQueryRepository.findRepresentativesByCorporateId(corporateId);
    }

    /**
     * 법인 대리인 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인 대리인 목록
     */
    @Transactional(readOnly = true)
    public List<AdminCorporateAgentResponse> getCorporateAgents(Long corporateId) {
        validateCorporateExists(corporateId);
        return additionalInfoQueryRepository.findAgentsByCorporateId(corporateId);
    }

    /**
     * 법인문서 목록을 조회합니다.
     *
     * @param corporateId 법인 ID
     * @return 법인문서 목록
     */
    @Transactional(readOnly = true)
    public List<AdminCorporateDocumentResponse> getCorporateDocuments(Long corporateId) {
        validateCorporateExists(corporateId);
        return additionalInfoQueryRepository.findDocumentsByCorporateId(corporateId);
    }

    private void validateSearchEnums(AdminCorporateUserSearchRequest request) {
        if (StringUtils.hasText(request.status())) {
            parseUserStatus(request.status());
        }
        if (StringUtils.hasText(request.kycStatus())) {
            parseKycStatus(request.kycStatus());
        }
    }

    private void validateCorporateUser(User user) {
        if (KyvcEnums.UserType.CORPORATE_USER != user.getUserTypeCode()) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateCorporateExists(Long corporateId) {
        // 법인 부가정보 조회 전, 잘못된 corporateId 요청을 명확히 구분하기 위해 법인 존재 여부를 먼저 확인한다.
        corporateRepository.findCorporateById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
    }

    private KyvcEnums.UserStatus parseUserStatus(String status) {
        try {
            return KyvcEnums.UserStatus.valueOf(status);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 사용자 상태입니다.");
        }
    }

    private KyvcEnums.KycStatus parseKycStatus(String kycStatus) {
        try {
            return KyvcEnums.KycStatus.valueOf(kycStatus);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 KYC 상태입니다.");
        }
    }

    private boolean shouldRevokeRefreshTokens(KyvcEnums.UserStatus status) {
        return KyvcEnums.UserStatus.LOCKED == status
                || KyvcEnums.UserStatus.INACTIVE == status
                || KyvcEnums.UserStatus.WITHDRAWN == status;
    }

    private String buildStatusChangeDescription(
            KyvcEnums.UserStatus beforeStatus,
            KyvcEnums.UserStatus afterStatus,
            String reason
    ) {
        String description = "법인 사용자 상태가 %s에서 %s로 변경되었습니다.".formatted(beforeStatus, afterStatus);
        if (StringUtils.hasText(reason)) {
            return description + " 사유: " + reason;
        }
        return description;
    }
}
