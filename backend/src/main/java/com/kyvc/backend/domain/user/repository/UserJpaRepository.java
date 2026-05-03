package com.kyvc.backend.domain.user.repository;

import com.kyvc.backend.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 JPA Repository
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {

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
     * 이메일 중복 여부 확인
     *
     * @param email 사용자 이메일
     * @return 이메일 존재 여부
     */
    boolean existsByEmail(
            String email // 사용자 이메일
    );
}
