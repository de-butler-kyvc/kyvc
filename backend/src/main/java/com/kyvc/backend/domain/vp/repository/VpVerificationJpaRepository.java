package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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
     * QR 토큰 해시 기준 조회
     *
     * @param qrTokenHash QR 토큰 해시
     * @return VP 검증 조회 결과
     */
    Optional<VpVerification> findByQrTokenHash(
            String qrTokenHash // QR 토큰 해시
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

    /**
     * 금융기관 코드 기준 VP 검증 요청 목록 조회
     *
     * @param financeInstitutionCode 금융기관 코드
     * @return VP 검증 요청 목록
     */
    List<VpVerification> findAllByFinanceInstitutionCodeOrderByRequestedAtDesc(
            String financeInstitutionCode // 금융기관 코드
    );

    /**
     * 금융기관 코드 + VP 요청 ID 기준 조회
     *
     * @param financeInstitutionCode 금융기관 코드
     * @param vpRequestId VP 요청 ID
     * @return VP 검증 요청 조회 결과
     */
    Optional<VpVerification> findByFinanceInstitutionCodeAndVpRequestId(
            String financeInstitutionCode, // 금융기관 코드
            String vpRequestId // VP 요청 ID
    );

    /**
     * Verifier 테스트 여부 기준 목록 조회
     *
     * @param verifierId Verifier ID
     * @param testYn 테스트 여부
     * @return VP 검증 테스트 목록
     */
    List<VpVerification> findAllByVerifierIdAndTestYnOrderByRequestedAtDesc(
            Long verifierId, // Verifier ID
            KyvcEnums.Yn testYn // 테스트 여부
    );

    /**
     * Verifier 테스트 검증 ID 기준 조회
     *
     * @param verifierId Verifier ID
     * @param vpVerificationId VP 검증 ID
     * @param testYn 테스트 여부
     * @return VP 테스트 검증 조회 결과
     */
    Optional<VpVerification> findByVerifierIdAndVpVerificationIdAndTestYn(
            Long verifierId, // Verifier ID
            Long vpVerificationId, // VP 검증 ID
            KyvcEnums.Yn testYn // 테스트 여부
    );
}
