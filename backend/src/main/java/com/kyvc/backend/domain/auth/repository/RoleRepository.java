package com.kyvc.backend.domain.auth.repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 역할 Repository
 */
public interface RoleRepository {

    /**
     * 사용자 ID 기준 활성 역할 목록 조회
     *
     * @param userId 사용자 ID
     * @return 활성 역할 목록
     */
    List<RoleRow> findEnabledRolesByUserId(
            Long userId // 사용자 ID
    );

    /**
     * 역할 코드 기준 활성 역할 조회
     *
     * @param roleCode 역할 코드
     * @return 활성 역할 조회 결과
     */
    Optional<RoleRow> findEnabledByRoleCode(
            String roleCode // 역할 코드
    );

    /**
     * 역할 조회 행
     *
     * @param roleId 역할 ID
     * @param roleCode 역할 코드
     * @param roleName 역할명
     * @param roleTypeCode 역할 유형 코드
     */
    record RoleRow(
            Long roleId,
            String roleCode,
            String roleName,
            String roleTypeCode
    ) {
    }
}
