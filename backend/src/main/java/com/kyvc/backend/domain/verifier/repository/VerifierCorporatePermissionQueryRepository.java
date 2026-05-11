package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.dto.VerifierCorporatePermissionListResponse;

import java.util.List;

/**
 * Verifier 기업 권한 QueryRepository
 */
public interface VerifierCorporatePermissionQueryRepository {

    /**
     * 기업 권한 확인 목록 조회
     *
     * @param verifierId Verifier ID
     * @param corporateId 법인 ID
     * @param permissionCode 권한 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 기업 권한 확인 목록
     */
    List<VerifierCorporatePermissionListResponse.Item> findPermissions(
            Long verifierId, // Verifier ID
            Long corporateId, // 법인 ID
            String permissionCode, // 권한 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 기업 권한 확인 건수 조회
     *
     * @param verifierId Verifier ID
     * @param corporateId 법인 ID
     * @param permissionCode 권한 코드
     * @return 기업 권한 확인 건수
     */
    long countPermissions(
            Long verifierId, // Verifier ID
            Long corporateId, // 법인 ID
            String permissionCode // 권한 코드
    );
}
