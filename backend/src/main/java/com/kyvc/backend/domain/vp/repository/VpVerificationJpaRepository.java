package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * VP 검증 JPA Repository
 */
public interface VpVerificationJpaRepository extends JpaRepository<VpVerification, Long> {

    /**
     * VP 검증 ID 기준 조회
     *
     * @param vpVerificationId VP 검증 ID
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByVpVerificationId(
            Long vpVerificationId // VP 검증 ID
    );

    /**
     * VP 요청 ID 기준 조회
     *
     * @param vpRequestId VP 요청 ID
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByVpRequestId(
            String vpRequestId // VP 요청 ID
    );

    /**
     * Core 요청 ID 기준 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByCoreRequestId(
            String coreRequestId // Core 요청 ID
    );

    /**
     * nonce + VP JWT 해시 기준 재사용 의심 여부 조회
     *
     * @param requestNonce 요청 nonce
     * @param vpJwtHash VP JWT 해시
     * @return 재사용 의심 여부
     */
    boolean existsByRequestNonceAndVpJwtHash(
            String requestNonce, // 요청 nonce
            String vpJwtHash // VP JWT 해시
    );
}
