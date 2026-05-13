package com.kyvc.backend.domain.mobile.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;
import com.kyvc.backend.domain.mobile.domain.MobileSecuritySetting;
import com.kyvc.backend.domain.mobile.dto.MobileSecuritySettingRequest;
import com.kyvc.backend.domain.mobile.dto.MobileSecuritySettingResponse;
import com.kyvc.backend.domain.mobile.repository.MobileDeviceBindingRepository;
import com.kyvc.backend.domain.mobile.repository.MobileSecuritySettingRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 모바일 보안 설정 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class MobileSecurityService {

    private static final String MOBILE_SECURITY_SETTING_UPDATE_ACTION = "MOBILE_SECURITY_SETTING_UPDATE"; // 모바일 보안 설정 변경 감사로그 작업 유형

    private final MobileDeviceBindingRepository mobileDeviceBindingRepository;
    private final MobileSecuritySettingRepository mobileSecuritySettingRepository;
    private final AuditLogService auditLogService;

    // 모바일 보안 설정 조회
    @Transactional(readOnly = true)
    public MobileSecuritySettingResponse getSecuritySetting(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    ) {
        validateUserId(userId);
        validateDeviceId(deviceId);

        findOwnedDeviceBinding(userId, deviceId);

        return mobileSecuritySettingRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(this::toResponse)
                .orElseGet(() -> new MobileSecuritySettingResponse(
                        deviceId,
                        false,
                        false,
                        null
                ));
    }

    // 모바일 보안 설정 저장
    public MobileSecuritySettingResponse updateSecuritySetting(
            Long userId, // 사용자 ID
            MobileSecuritySettingRequest request // 모바일 보안 설정 저장 요청
    ) {
        validateUserId(userId);
        validateSecuritySettingRequest(request);

        findOwnedDeviceBinding(userId, request.deviceId());

        MobileSecuritySetting securitySetting = mobileSecuritySettingRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .map(existingSetting -> updateExistingSetting(existingSetting, request))
                .orElseGet(() -> mobileSecuritySettingRepository.save(
                        MobileSecuritySetting.create(
                                userId,
                                request.deviceId(),
                                request.pinEnabled(),
                                request.biometricEnabled()
                        )
                ));

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                MOBILE_SECURITY_SETTING_UPDATE_ACTION,
                KyvcEnums.AuditTargetType.USER.name(),
                userId,
                "모바일 보안 설정 변경",
                null
        ));

        return toResponse(securitySetting);
    }

    // 기존 보안 설정 갱신
    private MobileSecuritySetting updateExistingSetting(
            MobileSecuritySetting securitySetting, // 기존 모바일 보안 설정
            MobileSecuritySettingRequest request // 모바일 보안 설정 저장 요청
    ) {
        securitySetting.update(request.pinEnabled(), request.biometricEnabled());
        return mobileSecuritySettingRepository.save(securitySetting);
    }

    // 소유 모바일 기기 조회
    private MobileDeviceBinding findOwnedDeviceBinding(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    ) {
        MobileDeviceBinding deviceBinding = mobileDeviceBindingRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new ApiException(ErrorCode.MOBILE_DEVICE_NOT_FOUND));
        if (!deviceBinding.isOwner(userId)) {
            throw new ApiException(ErrorCode.MOBILE_DEVICE_ACCESS_DENIED);
        }
        return deviceBinding;
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 모바일 기기 ID 검증
    private void validateDeviceId(
            String deviceId // 모바일 기기 ID
    ) {
        if (!StringUtils.hasText(deviceId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 모바일 보안 설정 저장 요청 검증
    private void validateSecuritySettingRequest(
            MobileSecuritySettingRequest request // 모바일 보안 설정 저장 요청
    ) {
        if (request == null || !StringUtils.hasText(request.deviceId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 모바일 보안 설정 응답 변환
    private MobileSecuritySettingResponse toResponse(
            MobileSecuritySetting securitySetting // 모바일 보안 설정 Entity
    ) {
        return new MobileSecuritySettingResponse(
                securitySetting.getDeviceId(),
                securitySetting.isPinEnabled(),
                securitySetting.isBiometricEnabled(),
                securitySetting.getUpdatedAt()
        );
    }
}
