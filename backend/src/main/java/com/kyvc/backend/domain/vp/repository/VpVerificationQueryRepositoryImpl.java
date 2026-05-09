package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// VP 검증 QueryRepository 구현체
@Repository
@RequiredArgsConstructor
public class VpVerificationQueryRepositoryImpl implements VpVerificationQueryRepository {

    private final VpVerificationJpaRepository vpVerificationJpaRepository;

    // 금융기관 VP 요청 목록 조회
    @Override
    public List<VpVerification> findFinanceRequests(
            String financeInstitutionCode, // 금융기관 코드
            KyvcEnums.VpVerificationStatus status, // VP 검증 상태
            LocalDateTime from, // 조회 시작 일시
            LocalDateTime to // 조회 종료 일시
    ) {
        return vpVerificationJpaRepository
                .findAllByFinanceInstitutionCodeOrderByRequestedAtDesc(financeInstitutionCode)
                .stream()
                .filter(vpVerification -> status == null || status == vpVerification.getVpVerificationStatus())
                .filter(vpVerification -> from == null || !vpVerification.getRequestedAt().isBefore(from))
                .filter(vpVerification -> to == null || !vpVerification.getRequestedAt().isAfter(to))
                .toList();
    }

    // 금융기관 VP 요청 상세 조회
    @Override
    public Optional<VpVerification> findFinanceRequest(
            String financeInstitutionCode, // 금융기관 코드
            String requestId // VP 요청 ID
    ) {
        return vpVerificationJpaRepository.findByFinanceInstitutionCodeAndVpRequestId(
                financeInstitutionCode,
                requestId
        );
    }

    // Verifier 테스트 검증 목록 조회
    @Override
    public List<VpVerification> findVerifierTests(
            Long verifierId // Verifier ID
    ) {
        return vpVerificationJpaRepository.findAllByVerifierIdAndTestYnOrderByRequestedAtDesc(
                verifierId,
                KyvcEnums.Yn.Y
        );
    }

    // Verifier 테스트 검증 상세 조회
    @Override
    public Optional<VpVerification> findVerifierTest(
            Long verifierId, // Verifier ID
            Long testId // 테스트 검증 ID
    ) {
        return vpVerificationJpaRepository.findByVerifierIdAndVpVerificationIdAndTestYn(
                verifierId,
                testId,
                KyvcEnums.Yn.Y
        );
    }
}
