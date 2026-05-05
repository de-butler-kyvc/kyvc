package com.kyvc.backend.domain.notification.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.notification.domain.Notification;
import com.kyvc.backend.domain.notification.dto.NotificationPageResponse;
import com.kyvc.backend.domain.notification.dto.NotificationResponse;
import com.kyvc.backend.domain.notification.dto.NotificationUnreadCountResponse;
import com.kyvc.backend.domain.notification.repository.NotificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 알림 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuditLogService auditLogService;

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(
            Long userId, // 사용자 ID
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        validateUserId(userId);

        int normalizedPage = normalizePage(page); // 보정 페이지 번호
        int normalizedSize = normalizeSize(size); // 보정 페이지 크기
        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        ); // 페이지 요청 정보

        Page<Notification> notificationPage = notificationRepository.findPageByUserId(userId, pageable);
        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new NotificationPageResponse(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages()
        );
    }

    // 읽지 않은 알림 수 조회
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);
        return new NotificationUnreadCountResponse(notificationRepository.countUnreadByUserId(userId));
    }

    // 알림 읽음 처리
    public NotificationResponse markAsRead(
            Long userId, // 사용자 ID
            Long notificationId // 알림 ID
    ) {
        validateUserId(userId);
        validateNotificationId(notificationId);

        Notification notification = findOwnedNotification(userId, notificationId);
        if (notification.isRead()) {
            return toResponse(notification);
        }

        notification.markAsRead();
        Notification savedNotification = notificationRepository.save(notification);

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                KyvcEnums.AuditActionType.NOTIFICATION_READ.name(),
                KyvcEnums.AuditTargetType.NOTIFICATION.name(),
                notificationId,
                "알림 읽음 처리",
                null
        ));

        return toResponse(savedNotification);
    }

    // 알림 전체 읽음 처리
    public NotificationUnreadCountResponse markAllAsRead(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);

        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                KyvcEnums.AuditActionType.NOTIFICATION_READ_ALL.name(),
                KyvcEnums.AuditTargetType.NOTIFICATION.name(),
                userId,
                "알림 전체 읽음 처리",
                null
        ));

        return new NotificationUnreadCountResponse(notificationRepository.countUnreadByUserId(userId));
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 알림 ID 검증
    private void validateNotificationId(
            Long notificationId // 알림 ID
    ) {
        if (notificationId == null || notificationId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 페이지 번호 보정
    private int normalizePage(
            int page // 원본 페이지 번호
    ) {
        return Math.max(page, 0);
    }

    // 페이지 크기 보정
    private int normalizeSize(
            int size // 원본 페이지 크기
    ) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    // 소유 알림 조회
    private Notification findOwnedNotification(
            Long userId, // 사용자 ID
            Long notificationId // 알림 ID
    ) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwner(userId)) {
            throw new ApiException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }
        return notification;
    }

    // 알림 응답 변환
    private NotificationResponse toResponse(
            Notification notification // 알림 Entity
    ) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
