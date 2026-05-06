package com.kyvc.backend.domain.mobile.repository;

import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 모바일 기기 바인딩 JPA Repository
public interface MobileDeviceBindingJpaRepository extends JpaRepository<MobileDeviceBinding, Long> {

    // 사용자 기기 단건 조회
    Optional<MobileDeviceBinding> findByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    );

    // 사용자 기기 목록 조회
    List<MobileDeviceBinding> findByUserIdOrderByLastUsedAtDesc(
            Long userId // 사용자 ID
    );

    // 사용자 기기 존재 여부 조회
    boolean existsByUserIdAndDeviceId(
            Long userId, // 사용자 ID
            String deviceId // 모바일 기기 ID
    );
}
