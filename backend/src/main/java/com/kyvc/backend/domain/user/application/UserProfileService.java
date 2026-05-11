package com.kyvc.backend.domain.user.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backend.domain.auth.repository.RoleRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.dto.UserMeResponse;
import com.kyvc.backend.domain.user.dto.UserMeUpdateRequest;
import com.kyvc.backend.domain.user.dto.UserMfaUpdateRequest;
import com.kyvc.backend.domain.user.dto.UserPasswordChangeRequest;
import com.kyvc.backend.domain.user.dto.UserProfileUpdateResponse;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

// 사용자 마이페이지 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserMeResponse getMe(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);
        return toResponse(findUser(userId));
    }

    // 내 기본 정보 수정
    public UserMeResponse updateMe(
            Long userId, // 사용자 ID
            UserMeUpdateRequest request // 내 정보 수정 요청
    ) {
        validateUserId(userId);
        validateUpdateRequest(request);

        User user = findUser(userId);
        user.updateProfile(
                resolveText(request.userName(), user.getUserName()),
                normalizeNullable(request.phone()),
                resolveNotificationYn(request.notificationEnabledYn(), user.getNotificationEnabledYn())
        );
        User savedUser = userRepository.save(user);
        saveAudit(userId, "USER_PROFILE_UPDATE", KyvcEnums.AuditTargetType.USER, userId, "사용자 기본 정보 수정");
        return toResponse(savedUser);
    }

    // 비밀번호 변경
    public UserProfileUpdateResponse changePassword(
            Long userId, // 사용자 ID
            UserPasswordChangeRequest request // 비밀번호 변경 요청
    ) {
        validateUserId(userId);
        validatePasswordRequest(request);

        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.CURRENT_PASSWORD_NOT_MATCHED);
        }
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new ApiException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCHED);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        saveAudit(userId, "USER_PASSWORD_CHANGE", KyvcEnums.AuditTargetType.USER, userId, "사용자 비밀번호 변경");
        return new UserProfileUpdateResponse(true);
    }

    // MFA 설정 변경
    public UserProfileUpdateResponse updateMfa(
            Long userId, // 사용자 ID
            UserMfaUpdateRequest request // MFA 설정 변경 요청
    ) {
        validateUserId(userId);
        validateMfaRequest(request);

        KyvcEnums.Yn mfaEnabledYn = parseYn(request.mfaEnabledYn()); // MFA 사용 여부
        if (KyvcEnums.Yn.Y == mfaEnabledYn) {
            verifyMfaSessionToken(userId, request.mfaToken());
        }

        User user = findUser(userId);
        user.updateMfaEnabled(mfaEnabledYn);
        userRepository.save(user);
        saveAudit(userId, "USER_MFA_UPDATE", KyvcEnums.AuditTargetType.USER, userId, "사용자 MFA 설정 변경");
        return new UserProfileUpdateResponse(true);
    }

    // 사용자 조회
    private User findUser(
            Long userId // 사용자 ID
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }
        return user;
    }

    // MFA 세션 토큰 검증
    private void verifyMfaSessionToken(
            Long userId, // 사용자 ID
            String mfaToken // MFA 세션 토큰
    ) {
        if (!StringUtils.hasText(mfaToken)) {
            throw new ApiException(ErrorCode.MFA_VERIFICATION_REQUIRED);
        }

        AuthToken authToken = authTokenRepository.findActiveByHashAndType(
                        TokenHashUtil.sha256(mfaToken),
                        KyvcEnums.TokenType.MFA_SESSION
                )
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_VERIFICATION_REQUIRED));
        if (!KyvcEnums.ActorType.USER.equals(authToken.getActorTypeCode()) || !userId.equals(authToken.getActorId())) {
            throw new ApiException(ErrorCode.MFA_VERIFICATION_REQUIRED);
        }
        LocalDateTime now = LocalDateTime.now(); // 검증 기준 일시
        if (authToken.isExpired(now)) {
            authToken.markUsed(now);
            authTokenRepository.save(authToken);
            throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }
        authToken.markUsed(now);
        authTokenRepository.save(authToken);
    }

    // 응답 변환
    private UserMeResponse toResponse(
            User user // 사용자 엔티티
    ) {
        List<UserMeResponse.RoleItem> roles = roleRepository.findEnabledRolesByUserId(user.getUserId()).stream()
                .map(role -> new UserMeResponse.RoleItem(
                        role.roleCode(),
                        role.roleName(),
                        role.roleTypeCode()
                ))
                .toList();
        return new UserMeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUserName(),
                user.getPhone(),
                user.getUserTypeCode().name(),
                user.getUserStatusCode().name(),
                enumName(user.getNotificationEnabledYn()),
                enumName(user.getMfaEnabledYn()),
                user.getMfaTypeCode(),
                user.getLastPasswordChangedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roles
        );
    }

    // 감사로그 저장
    private void saveAudit(
            Long userId, // 사용자 ID
            String actionType, // 작업 유형
            KyvcEnums.AuditTargetType targetType, // 감사 대상 유형
            Long targetId, // 대상 ID
            String summary // 요청 요약
    ) {
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                actionType,
                targetType.name(),
                targetId,
                summary,
                null
        ));
    }

    // 내 정보 수정 요청 검증
    private void validateUpdateRequest(
            UserMeUpdateRequest request // 내 정보 수정 요청
    ) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (request.userName() != null && !StringUtils.hasText(request.userName())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (request.notificationEnabledYn() != null) {
            parseYn(request.notificationEnabledYn());
        }
    }

    // 비밀번호 변경 요청 검증
    private void validatePasswordRequest(
            UserPasswordChangeRequest request // 비밀번호 변경 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.currentPassword())
                || !StringUtils.hasText(request.newPassword())
                || !StringUtils.hasText(request.newPasswordConfirm())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // MFA 설정 변경 요청 검증
    private void validateMfaRequest(
            UserMfaUpdateRequest request // MFA 설정 변경 요청
    ) {
        if (request == null || !StringUtils.hasText(request.mfaEnabledYn())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        parseYn(request.mfaEnabledYn());
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // Y/N 변환
    private KyvcEnums.Yn parseYn(
            String value // Y/N 원본 값
    ) {
        try {
            return KyvcEnums.Yn.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 알림 여부 산정
    private KyvcEnums.Yn resolveNotificationYn(
            String notificationEnabledYn, // 요청 알림 수신 여부
            KyvcEnums.Yn currentValue // 현재 알림 수신 여부
    ) {
        if (!StringUtils.hasText(notificationEnabledYn)) {
            return currentValue;
        }
        return parseYn(notificationEnabledYn);
    }

    // 문자열 변경값 산정
    private String resolveText(
            String requestValue, // 요청 문자열
            String currentValue // 현재 문자열
    ) {
        if (requestValue == null) {
            return currentValue;
        }
        return requestValue.trim();
    }

    // 선택 문자열 정규화
    private String normalizeNullable(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
