package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// VP 검증 Repository 구현체
@Repository
@RequiredArgsConstructor
public class VpVerificationRepositoryImpl implements VpVerificationRepository {

    private final VpVerificationJpaRepository vpVerificationJpaRepository;

    // VP 검증 저장
    @Override
    public VpVerification save(
            VpVerification vpVerification // 저장 대상 VP 검증
    ) {
        return vpVerificationJpaRepository.save(vpVerification);
    }

    // VP 검증 ID 기준 조회
    @Override
    public VpVerification getById(
            Long vpVerificationId // VP 검증 ID
    ) {
        return vpVerificationJpaRepository.findByVpVerificationId(vpVerificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.VP_PRESENTATION_NOT_FOUND));
    }

    // VP 요청 ID 기준 조회
    @Override
    public VpVerification getByRequestId(
            String requestId // VP 요청 ID
    ) {
        return findByRequestId(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.VP_REQUEST_NOT_FOUND));
    }

    // VP 요청 ID 기준 Optional 조회
    @Override
    public Optional<VpVerification> findByRequestId(
            String requestId // VP 요청 ID
    ) {
        return vpVerificationJpaRepository.findByVpRequestId(requestId);
    }

    // Core 요청 ID 기준 Optional 조회
    @Override
    public Optional<VpVerification> findByCoreRequestId(
            String coreRequestId // Core 요청 ID
    ) {
        return vpVerificationJpaRepository.findByCoreRequestId(coreRequestId);
    }

    // 재사용 의심 후보 여부 조회
    @Override
    public boolean existsReplayCandidate(
            String requestNonce, // 요청 nonce
            String vpJwtHash // VP JWT 해시
    ) {
        return vpVerificationJpaRepository.existsByRequestNonceAndVpJwtHash(requestNonce, vpJwtHash);
    }
}
