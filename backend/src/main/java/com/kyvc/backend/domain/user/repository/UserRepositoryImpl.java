package com.kyvc.backend.domain.user.repository;

import com.kyvc.backend.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 사용자 Repository 구현체
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    // 이메일 기준 사용자 조회
    @Override
    public Optional<User> findByEmail(
            String email // 사용자 이메일
    ) {
        return userJpaRepository.findByEmail(email);
    }

    // ID 기준 사용자 조회
    @Override
    public Optional<User> findById(
            Long userId // 사용자 ID
    ) {
        return userJpaRepository.findById(userId);
    }

    // 이메일 중복 여부 확인
    @Override
    public boolean existsByEmail(
            String email // 사용자 이메일
    ) {
        return userJpaRepository.existsByEmail(email);
    }

    // 사용자 저장
    @Override
    public User save(
            User user // 저장 대상 사용자
    ) {
        return userJpaRepository.save(user);
    }
}
