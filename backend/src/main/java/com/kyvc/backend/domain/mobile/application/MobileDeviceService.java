package com.kyvc.backend.domain.mobile.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;
import com.kyvc.backend.domain.mobile.dto.MobileDeviceRegisterRequest;
import com.kyvc.backend.domain.mobile.dto.MobileDeviceRegisterResponse;
import com.kyvc.backend.domain.mobile.repository.MobileDeviceBindingRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

// 모바일 기기 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class MobileDeviceService {

    private static final String MOBILE_DEVICE_REGISTER_ACTION = "MOBILE_DEVICE_REGISTER"; // 모바일 기기 등록 감사로그 작업 유형

    private final MobileDeviceBindingRepository mobileDeviceBindingRepository;
    private final AuditLogService auditLogService;

    // 모바일 기기 등록
    public MobileDeviceRegisterResponse registerDevice(
            Long userId, // 사용자 ID
            MobileDeviceRegisterRequest request // 모바일 기기 등록 요청
    ) {
        validateUserId(userId);
        validateRegisterRequest(request);

        LocalDateTime now = LocalDateTime.now(); // 기준 일시
        MobileDeviceBinding savedDeviceBinding = mobileDeviceBindingRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .map(deviceBinding -> updateExistingDevice(deviceBinding, request, now))
                .orElseGet(() -> mobileDeviceBindingRepository.save(MobileDeviceBinding.create(userId, request, now)));

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                MOBILE_DEVICE_REGISTER_ACTION,
                KyvcEnums.AuditTargetType.USER.name(),
                userId,
                "모바일 기기 등록",
                null
        ));

        return toResponse(savedDeviceBinding);
    }

    // 기존 기기 정보 갱신
    private MobileDeviceBinding updateExistingDevice(
            MobileDeviceBinding deviceBinding, // 기존 모바일 기기
            MobileDeviceRegisterRequest request, // 모바일 기기 등록 요청
            LocalDateTime now // 기준 일시
    ) {
        if (KyvcEnums.DeviceBindingStatus.BLOCKED == deviceBinding.getDeviceBindingStatus()) {
            throw new ApiException(ErrorCode.MOBILE_INVALID_DEVICE);
        }
        if (!deviceBinding.isActive() && KyvcEnums.DeviceBindingStatus.REMOVED != deviceBinding.getDeviceBindingStatus()) {
            throw new ApiException(ErrorCode.MOBILE_INVALID_DEVICE);
        }

        deviceBinding.updateDeviceInfo(request, now);
        return mobileDeviceBindingRepository.save(deviceBinding);
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 모바일 기기 등록 요청 검증
    private void validateRegisterRequest(
            MobileDeviceRegisterRequest request // 모바일 기기 등록 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.deviceId())
                || !StringUtils.hasText(request.os())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 모바일 기기 등록 응답 변환
    private MobileDeviceRegisterResponse toResponse(
            MobileDeviceBinding deviceBinding // 모바일 기기 Entity
    ) {
        return new MobileDeviceRegisterResponse(
                deviceBinding.getDeviceBindingId(),
                deviceBinding.getDeviceId(),
                deviceBinding.getDeviceName(),
                deviceBinding.getOs(),
                deviceBinding.getAppVersion(),
                deviceBinding.getDeviceBindingStatus().name(),
                deviceBinding.getRegisteredAt(),
                deviceBinding.getLastUsedAt()
        );
    }

    // 활성 모바일 기기 조회
    @Transactional(readOnly = true)
    public MobileDeviceBinding getActiveDeviceBinding(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    ) {
        validateUserId(userId);
        if (!StringUtils.hasText(deviceId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        MobileDeviceBinding deviceBinding = mobileDeviceBindingRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new ApiException(ErrorCode.WALLET_DEVICE_NOT_REGISTERED));
        if (!deviceBinding.isOwner(userId)) {
            throw new ApiException(ErrorCode.MOBILE_DEVICE_ACCESS_DENIED);
        }
        if (!deviceBinding.isActive()) {
            throw new ApiException(ErrorCode.WALLET_DEVICE_INACTIVE);
        }
        return deviceBinding;
    }
}
