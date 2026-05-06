package com.kyvc.backend.domain.mobile.repository;

import com.kyvc.backend.domain.mobile.domain.MobileSecuritySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 모바일 보안 설정 JPA Repository
public interface MobileSecuritySettingJpaRepository extends JpaRepository<MobileSecuritySetting, Long> {

    // 사용자 기기 보안 설정 조회
    Optional<MobileSecuritySetting> findByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    );
}
