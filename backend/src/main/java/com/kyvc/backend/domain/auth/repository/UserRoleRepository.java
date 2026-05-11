package com.kyvc.backend.domain.auth.repository;

/**
 * 사용자 보유 역할 Repository
 */
public interface UserRoleRepository {

    /**
     * 사용자 보유 활성 역할 존재 여부
     *
     * @param userId 사용자 ID
     * @param roleCode 역할 코드
     * @return 보유 여부
     */
    boolean existsEnabledUserRole(
            Long userId, // 사용자 ID
            String roleCode // 역할 코드
    );
}
