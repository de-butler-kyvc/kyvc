package com.kyvc.backend.domain.mobile.repository;

import com.kyvc.backend.domain.mobile.domain.MobileSecuritySetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 모바일 보안 설정 Repository 구현체
@Repository
@RequiredArgsConstructor
public class MobileSecuritySettingRepositoryImpl implements MobileSecuritySettingRepository {

    private final MobileSecuritySettingJpaRepository mobileSecuritySettingJpaRepository;

    // 사용자 기기 보안 설정 조회
    @Override
    public Optional<MobileSecuritySetting> findByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    ) {
        return mobileSecuritySettingJpaRepository.findByUserIdAndDeviceId(userId, deviceId);
    }

    // 모바일 보안 설정 저장
    @Override
    public MobileSecuritySetting save(
            MobileSecuritySetting securitySetting // 저장 대상 모바일 보안 설정
    ) {
        return mobileSecuritySettingJpaRepository.save(securitySetting);
    }
}
