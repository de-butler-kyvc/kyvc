package com.kyvc.backendadmin.domain.corporate.application;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.corporate.domain.Corporate;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserCreateRequest;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserCreateResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserDeleteResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserListResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserSearchRequest;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserStatusUpdateRequest;
import com.kyvc.backendadmin.domain.corporate.repository.CorporateQueryRepository;
import com.kyvc.backendadmin.domain.corporate.repository.CorporateRepository;
import com.kyvc.backendadmin.domain.user.domain.User;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 법인 사용자 목록/상세/상태변경과 법인 상세 조회 유스케이스를 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class AdminCorporateService {

    private static final Logger log = LoggerFactory.getLogger(AdminCorporateService.class);
    private static final List<KyvcEnums.UserStatus> CREATE_ALLOWED_STATUSES = List.of(
            KyvcEnums.UserStatus.ACTIVE,
            KyvcEnums.UserStatus.LOCKED,
            KyvcEnums.UserStatus.WITHDRAWN
    );

    private final CorporateRepository corporateRepository;
    private final CorporateQueryRepository corporateQueryRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 법인 사용자 목록을 검색합니다.
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
     * 법인 사용자 상세 정보를 조회합니다.
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
     * 법인 사용자 계정과 법인 정보를 등록합니다.
     *
     * <p>SYSTEM_ADMIN 또는 OPERATOR 권한을 확인하고 이메일, 비밀번호, 법인명, 사업자등록번호를 검증합니다.
     * 비밀번호는 BCrypt로 해시 저장하며, 응답과 감사로그에는 비밀번호 및 대표자 개인정보를 포함하지 않습니다.</p>
     *
     * @param request 법인 사용자 계정 등록 요청
     * @return 생성된 사용자 ID, 법인 ID와 생성 여부
     */
    @Transactional
    public AdminCorporateUserCreateResponse createUser(AdminCorporateUserCreateRequest request) {
        validateOperatorPermission();
        validateCreateRequest(request);
        if (corporateRepository.existsUserByEmail(request.email().trim())) {
            log.warn("법인 사용자 등록 실패: 중복 email");
            throw new ApiException(ErrorCode.DUPLICATED_EMAIL);
        }

        KyvcEnums.UserStatus userStatus = parseCreateUserStatus(request.status());
        User user = User.createCorporateUser(
                request.email().trim(),
                passwordEncoder.encode(request.password()),
                normalizeNullable(request.representativeName()),
                normalizeNullable(request.representativePhone()),
                userStatus
        );
        corporateRepository.saveUser(user);

        Corporate corporate = Corporate.create(
                user.getUserId(),
                request.corporateName().trim(),
                request.businessRegistrationNo().trim(),
                normalizeNullable(request.corporateRegistrationNo()),
                normalizeNullable(request.representativeName()),
                normalizeNullable(request.representativePhone()),
                normalizeNullable(request.representativeEmail()),
                normalizeNullable(request.address()),
                normalizeNullable(request.businessType()),
                KyvcEnums.CorporateStatus.PENDING
        );
        corporateRepository.saveCorporate(corporate);

        corporateRepository.saveAuditLog(AuditLog.user(
                SecurityUtil.getCurrentAdminId(),
                user.getUserId(),
                "CORPORATE_USER_CREATE",
                "법인 사용자 계정을 생성했습니다. userStatus=%s, corporateId=%d"
                        .formatted(userStatus, corporate.getCorporateId())
        ));
        corporateRepository.saveAuditLog(AuditLog.corporate(
                SecurityUtil.getCurrentAdminId(),
                corporate.getCorporateId(),
                "CORPORATE_CREATE",
                "법인 정보를 생성했습니다. userId=%d, corporateStatus=%s"
                        .formatted(user.getUserId(), corporate.getCorporateStatusCode())
        ));
        log.info("법인 사용자 등록 완료: userId={}, corporateId={}", user.getUserId(), corporate.getCorporateId());
        return new AdminCorporateUserCreateResponse(user.getUserId(), corporate.getCorporateId(), true);
    }

    /**
     * 법인 사용자 계정을 삭제 처리합니다.
     *
     * <p>물리 삭제는 수행하지 않고 users.user_status_code를 WITHDRAWN으로 변경합니다.
     * 진행 중인 KYC나 유효한 Credential이 있으면 삭제를 차단하고, 삭제 성공 시 해당 사용자의 활성 인증 토큰을 폐기합니다.</p>
     *
     * @param userId 삭제 처리할 사용자 ID
     * @return 삭제 처리된 사용자 ID와 삭제 여부
     */
    @Transactional
    public AdminCorporateUserDeleteResponse deleteUser(Long userId) {
        validateOperatorPermission();
        User user = corporateRepository.findUserById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateCorporateUserType(user);
        if (KyvcEnums.UserStatus.WITHDRAWN == user.getUserStatusCode()) {
            log.warn("법인 사용자 삭제 실패: 이미 탈퇴 처리 userId={}", userId);
            throw new ApiException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }
        if (corporateRepository.existsActiveKycByUserId(userId)) {
            log.warn("법인 사용자 삭제 실패: 진행 중 KYC 존재 userId={}", userId);
            throw new ApiException(ErrorCode.USER_HAS_ACTIVE_KYC);
        }
        if (corporateRepository.existsValidCredentialByUserId(userId)) {
            log.warn("법인 사용자 삭제 실패: 유효 Credential 존재 userId={}", userId);
            throw new ApiException(ErrorCode.USER_HAS_VALID_CREDENTIAL);
        }

        KyvcEnums.UserStatus beforeStatus = user.getUserStatusCode();
        user.changeStatus(KyvcEnums.UserStatus.WITHDRAWN);
        authTokenRepository.revokeActiveTokens(KyvcEnums.ActorType.USER, user.getUserId());
        corporateRepository.saveAuditLog(AuditLog.user(
                SecurityUtil.getCurrentAdminId(),
                user.getUserId(),
                "CORPORATE_USER_DELETE",
                "법인 사용자 계정을 삭제 처리했습니다. beforeStatus=%s, afterStatus=%s"
                        .formatted(beforeStatus, KyvcEnums.UserStatus.WITHDRAWN)
        ));
        log.info("법인 사용자 삭제 처리 완료: userId={}", userId);
        return new AdminCorporateUserDeleteResponse(userId, true);
    }

    /**
     * 법인 사용자 계정 상태를 변경합니다.
     *
     * <p>사용자 존재 여부와 CORPORATE_USER 유형을 확인한 뒤 요청 상태값을 검증합니다.
     * 잠금/비활성/탈퇴 상태로 변경하는 경우 기존 refresh token을 폐기하고,
     * 모든 상태 변경은 감사로그에 관리자 행위 이력으로 기록합니다.</p>
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

        // 검증된 상태값만 users.user_status_code에 반영합니다.
        user.changeStatus(afterStatus);

        // 비정상 상태로 전환될 때 기존 refresh token을 폐기해 재사용을 막습니다.
        if (shouldRevokeRefreshTokens(afterStatus)) {
            authTokenRepository.revokeActiveTokens(
                    KyvcEnums.ActorType.USER,
                    user.getUserId(),
                    KyvcEnums.TokenType.REFRESH
            );
        }

        // 상태 변경 이력은 감사로그로 남기되 비밀번호/토큰 등 민감정보는 포함하지 않습니다.
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
     * @param corporateId 조회할 법인 ID
     * @return 법인 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminCorporateDetailResponse getCorporateDetail(Long corporateId) {
        return corporateQueryRepository.findCorporateDetail(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
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

    private void validateCorporateUserType(User user) {
        if (KyvcEnums.UserType.CORPORATE_USER != user.getUserTypeCode()) {
            throw new ApiException(ErrorCode.INVALID_USER_TYPE);
        }
    }

    private void validateOperatorPermission() {
        if (!SecurityUtil.hasRole(KyvcEnums.RoleCode.SYSTEM_ADMIN.name())
                && !SecurityUtil.hasRole(KyvcEnums.RoleCode.OPERATOR.name())) {
            log.warn("법인 사용자 변경 권한 부족: adminId={}", SecurityUtil.getCurrentAdminId());
            throw new ApiException(ErrorCode.ADMIN_PERMISSION_DENIED);
        }
    }

    private void validateCreateRequest(AdminCorporateUserCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.email())
                || !StringUtils.hasText(request.password())
                || !StringUtils.hasText(request.corporateName())
                || !StringUtils.hasText(request.businessRegistrationNo())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private KyvcEnums.UserStatus parseCreateUserStatus(String status) {
        KyvcEnums.UserStatus userStatus = StringUtils.hasText(status)
                ? parseUserStatus(status.trim())
                : KyvcEnums.UserStatus.ACTIVE;
        if (!CREATE_ALLOWED_STATUSES.contains(userStatus)) {
            throw new ApiException(ErrorCode.INVALID_USER_STATUS);
        }
        return userStatus;
    }

    private KyvcEnums.UserStatus parseUserStatus(String status) {
        try {
            return KyvcEnums.UserStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_USER_STATUS);
        }
    }

    private KyvcEnums.KycStatus parseKycStatus(String kycStatus) {
        try {
            return KyvcEnums.KycStatus.valueOf(kycStatus);
        } catch (IllegalArgumentException exception) {
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

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
