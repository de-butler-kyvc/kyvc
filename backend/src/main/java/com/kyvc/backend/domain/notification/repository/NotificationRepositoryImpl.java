package com.kyvc.backend.domain.notification.repository;

import com.kyvc.backend.domain.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 알림 Repository 구현체
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private static final String READ_NO = "N"; // 읽지 않음 값

    private final NotificationJpaRepository notificationJpaRepository;

    // 사용자 알림 페이지 조회
    @Override
    public Page<Notification> findPageByUserId(
            Long userId, // 사용자 ID
            Pageable pageable // 페이지 정보
    ) {
        return notificationJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // 알림 ID 기준 조회
    @Override
    public Optional<Notification> findById(
            Long notificationId // 알림 ID
    ) {
        return notificationJpaRepository.findById(notificationId);
    }

    // 사용자 읽지 않은 알림 수 조회
    @Override
    public long countUnreadByUserId(
            Long userId // 사용자 ID
    ) {
        return notificationJpaRepository.countByUserIdAndReadYn(userId, READ_NO);
    }

    // 사용자 읽지 않은 알림 목록 조회
    @Override
    public List<Notification> findUnreadByUserId(
            Long userId // 사용자 ID
    ) {
        return notificationJpaRepository.findByUserIdAndReadYn(userId, READ_NO);
    }

    // 알림 저장
    @Override
    public Notification save(
            Notification notification // 저장 대상 알림
    ) {
        return notificationJpaRepository.save(notification);
    }

    // 알림 목록 저장
    @Override
    public List<Notification> saveAll(
            List<Notification> notifications // 저장 대상 알림 목록
    ) {
        return notificationJpaRepository.saveAll(notifications);
    }
}
