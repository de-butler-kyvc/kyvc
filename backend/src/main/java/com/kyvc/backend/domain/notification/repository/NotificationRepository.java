package com.kyvc.backend.domain.notification.repository;

import com.kyvc.backend.domain.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

// 알림 Repository
public interface NotificationRepository {

    // 사용자 알림 페이지 조회
    Page<Notification> findPageByUserId(
            Long userId, // 사용자 ID
            Pageable pageable // 페이지 정보
    );

    // 알림 ID 기준 조회
    Optional<Notification> findById(
            Long notificationId // 알림 ID
    );

    // 사용자 읽지 않은 알림 수 조회
    long countUnreadByUserId(
            Long userId // 사용자 ID
    );

    // 사용자 읽지 않은 알림 목록 조회
    List<Notification> findUnreadByUserId(
            Long userId // 사용자 ID
    );

    // 알림 저장
    Notification save(
            Notification notification // 저장 대상 알림
    );

    // 알림 목록 저장
    List<Notification> saveAll(
            List<Notification> notifications // 저장 대상 알림 목록
    );
}
