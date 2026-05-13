package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;

import java.util.Optional;

/**
 * VP 검증 Repository
 */
public interface VpVerificationRepository {

    /**
     * VP 검증 저장
     *
     * @param vpVerification 저장 대상 VP 검증
     * @return 저장된 VP 검증
     */
    VpVerification save(
            VpVerification vpVerification // 저장 대상 VP 검증
    );

    /**
     * VP 검증 ID 기준 조회
     *
     * @param vpVerificationId VP 검증 ID
     * @return VP 검증
     */
    VpVerification getById(
            Long vpVerificationId // VP 검증 ID
    );

    /**
     * VP 요청 ID 기준 조회
     *
     * @param requestId VP 요청 ID
     * @return VP 검증
     */
    VpVerification getByRequestId(
            String requestId // VP 요청 ID
    );

    /**
     * VP 요청 ID 기준 Optional 조회
     *
     * @param requestId VP 요청 ID
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByRequestId(
            String requestId // VP 요청 ID
    );

    /**
     * Core 요청 ID 기준 Optional 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByCoreRequestId(
            String coreRequestId // Core 요청 ID
    );

    /**
     * QR 토큰 해시 기준 Optional 조회
     *
     * @param qrTokenHash QR 토큰 해시
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByQrTokenHash(
            String qrTokenHash // QR 토큰 해시
    );

    /**
     * 재사용 의심 후보 여부 조회
     *
     * @param requestNonce 요청 nonce
     * @param vpJwtHash VP JWT 해시
     * @return 재사용 의심 후보 여부
     */
    boolean existsReplayCandidate(
            String requestNonce, // 요청 nonce
            String vpJwtHash // VP JWT 해시
    );
}
