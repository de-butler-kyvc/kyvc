package com.kyvc.backend.domain.mobile.repository;

import com.kyvc.backend.domain.mobile.domain.MobileSecuritySetting;

import java.util.Optional;

// 모바일 보안 설정 Repository
public interface MobileSecuritySettingRepository {

    // 사용자 기기 보안 설정 조회
    Optional<MobileSecuritySetting> findByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    );

    // 모바일 보안 설정 저장
    MobileSecuritySetting save(
            MobileSecuritySetting securitySetting // 저장 대상 모바일 보안 설정
    );
}
