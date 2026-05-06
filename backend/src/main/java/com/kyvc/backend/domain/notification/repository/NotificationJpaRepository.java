package com.kyvc.backend.domain.notification.repository;

import com.kyvc.backend.domain.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 알림 JPA Repository
public interface NotificationJpaRepository extends JpaRepository<Notification, Long> {

    // 사용자 알림 페이지 조회
    Page<Notification> findByUserIdOrderByCreatedAtDesc(
            Long userId, // 사용자 ID
            Pageable pageable // 페이지 정보
    );

    // 사용자 읽지 않은 알림 수 조회
    long countByUserIdAndReadYn(
            Long userId, // 사용자 ID
            String readYn // 읽음 여부
    );

    // 사용자 읽지 않은 알림 목록 조회
    List<Notification> findByUserIdAndReadYn(
            Long userId, // 사용자 ID
            String readYn // 읽음 여부
    );
}
