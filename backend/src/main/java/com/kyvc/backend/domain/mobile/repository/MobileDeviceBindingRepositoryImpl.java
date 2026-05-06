package com.kyvc.backend.domain.mobile.repository;

import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 모바일 기기 바인딩 Repository 구현체
@Repository
@RequiredArgsConstructor
public class MobileDeviceBindingRepositoryImpl implements MobileDeviceBindingRepository {

    private final MobileDeviceBindingJpaRepository mobileDeviceBindingJpaRepository;

    // 사용자 기기 단건 조회
    @Override
    public Optional<MobileDeviceBinding> findByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    ) {
        return mobileDeviceBindingJpaRepository.findByUserIdAndDeviceId(userId, deviceId);
    }

    // 사용자 기기 목록 조회
    @Override
    public List<MobileDeviceBinding> findByUserId(
            Long userId // 사용자 ID
    ) {
        return mobileDeviceBindingJpaRepository.findByUserIdOrderByLastUsedAtDesc(userId);
    }

    // 사용자 기기 존재 여부 조회
    @Override
    public boolean existsByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    ) {
        return mobileDeviceBindingJpaRepository.existsByUserIdAndDeviceId(userId, deviceId);
    }

    // 모바일 기기 저장
    @Override
    public MobileDeviceBinding save(
            MobileDeviceBinding deviceBinding // 저장 대상 모바일 기기
    ) {
        return mobileDeviceBindingJpaRepository.save(deviceBinding);
    }
}
