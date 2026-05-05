package com.kyvc.backend.domain.mobile.repository;

import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;

import java.util.List;
import java.util.Optional;

// 모바일 기기 바인딩 Repository
public interface MobileDeviceBindingRepository {

    // 사용자 기기 단건 조회
    Optional<MobileDeviceBinding> findByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    );

    // 사용자 기기 목록 조회
    List<MobileDeviceBinding> findByUserId(
            Long userId // 사용자 ID
    );

    // 사용자 기기 존재 여부 조회
    boolean existsByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    );

    // 모바일 기기 저장
    MobileDeviceBinding save(
            MobileDeviceBinding deviceBinding // 저장 대상 모바일 기기
    );
}
