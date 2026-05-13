package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.util.KyvcEnums;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * VP 검증 QueryRepository
 */
public interface VpVerificationQueryRepository {

    /**
     * 금융기관 VP 요청 목록 조회
     *
     * @param financeInstitutionCode 금융기관 코드
     * @param status VP 검증 상태
     * @param from 조회 시작 일시
     * @param to 조회 종료 일시
     * @return VP 요청 목록
     */
    List<VpVerification> findFinanceRequests(
            String financeInstitutionCode, // 금융기관 코드
            KyvcEnums.VpVerificationStatus status, // VP 검증 상태
            LocalDateTime from, // 조회 시작 일시
            LocalDateTime to // 조회 종료 일시
    );

    /**
     * 금융기관 VP 요청 상세 조회
     *
     * @param financeInstitutionCode 금융기관 코드
     * @param requestId VP 요청 ID
     * @return VP 요청 조회 결과
     */
    Optional<VpVerification> findFinanceRequest(
            String financeInstitutionCode, // 금융기관 코드
            String requestId // VP 요청 ID
    );

    /**
     * Verifier 테스트 검증 목록 조회
     *
     * @param verifierId Verifier ID
     * @return 테스트 검증 목록
     */
    List<VpVerification> findVerifierTests(
            Long verifierId // Verifier ID
    );

    /**
     * Verifier 테스트 검증 상세 조회
     *
     * @param verifierId Verifier ID
     * @param testId 테스트 검증 ID
     * @return 테스트 검증 조회 결과
     */
    Optional<VpVerification> findVerifierTest(
            Long verifierId, // Verifier ID
            Long testId // 테스트 검증 ID
    );
}
