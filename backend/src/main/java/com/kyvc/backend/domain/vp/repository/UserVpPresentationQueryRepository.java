package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.List;

/**
 * 사용자 VP 제출 이력 조회 Repository
 */
public interface UserVpPresentationQueryRepository {

    /**
     * 사용자 소유 VP 제출 이력 목록 조회
     *
     * @param userId 사용자 ID
     * @param status 검증 상태
     * @param verifierName Verifier명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return VP 제출 이력 목록
     */
    List<VpVerification> findByUserId(
            Long userId, // 사용자 ID
            KyvcEnums.VpVerificationStatus status, // 검증 상태
            String verifierName, // Verifier명
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 사용자 소유 VP 제출 이력 건수 조회
     *
     * @param userId 사용자 ID
     * @param status 검증 상태
     * @param verifierName Verifier명
     * @return 전체 건수
     */
    long countByUserId(
            Long userId, // 사용자 ID
            KyvcEnums.VpVerificationStatus status, // 검증 상태
            String verifierName // Verifier명
    );
}
