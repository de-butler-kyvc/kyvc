package com.kyvc.backend.domain.user.repository;

import com.kyvc.backend.domain.user.domain.User;

import java.util.Optional;

/**
 * 사용자 Repository
 */
public interface UserRepository {

    /**
     * 이메일 기준 사용자 조회
     *
     * @param email 사용자 이메일
     * @return 사용자 조회 결과
     */
    Optional<User> findByEmail(
            String email // 사용자 이메일
    );

    /**
     * ID 기준 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 조회 결과
     */
    Optional<User> findById(
            Long userId // 사용자 ID
    );

    /**
     * 이메일 중복 여부 확인
     *
     * @param email 사용자 이메일
     * @return 이메일 존재 여부
     */
    boolean existsByEmail(
            String email // 사용자 이메일
    );

    /**
     * 사용자 저장
     *
     * @param user 저장 대상 사용자
     * @return 저장된 사용자
     */
    User save(
            User user // 저장 대상 사용자
    );
}
