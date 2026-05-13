package com.kyvc.backend.domain.notification.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.notification.config.NotificationProperties;
import com.kyvc.backend.domain.notification.domain.Notification;
import com.kyvc.backend.domain.notification.domain.NotificationTemplate;
import com.kyvc.backend.domain.notification.dto.InternalNotificationBulkSendRequest;
import com.kyvc.backend.domain.notification.dto.InternalNotificationBulkSendResponse;
import com.kyvc.backend.domain.notification.dto.InternalNotificationSendRequest;
import com.kyvc.backend.domain.notification.dto.InternalNotificationSendResponse;
import com.kyvc.backend.domain.notification.dto.NotificationPageResponse;
import com.kyvc.backend.domain.notification.dto.NotificationResponse;
import com.kyvc.backend.domain.notification.dto.NotificationUnreadCountResponse;
import com.kyvc.backend.domain.notification.repository.NotificationRepository;
import com.kyvc.backend.domain.notification.repository.NotificationTemplateRepository;
import com.kyvc.backend.domain.user.repository.UserRepository;
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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// 알림 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private static final String INTERNAL_NOTIFICATION_SEND_ACTION = "INTERNAL_NOTIFICATION_SEND"; // 내부 알림 발송 작업 유형
    private static final String INTERNAL_NOTIFICATION_SEND_SUMMARY = "내부 알림 발송 요청 저장"; // 내부 알림 발송 요약
    private static final String INTERNAL_NOTIFICATION_SEND_STATUS = "SAVED"; // 내부 알림 발송 상태

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationProperties notificationProperties;

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

    // 내부 알림 발송 저장
    public InternalNotificationSendResponse sendInternalNotification(
            InternalNotificationSendRequest request // 내부 알림 발송 요청
    ) {
        validateInternalNotificationRequest(request);

        String notificationType = normalizeNotificationType(request.type());
        Notification notification = Notification.create(
                request.recipientUserId(),
                notificationType,
                request.title().trim(),
                request.message().trim()
        );

        Notification savedNotification;
        try {
            savedNotification = notificationRepository.save(notification);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_NOTIFICATION_SAVE_FAILED);
        }

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.SYSTEM.name(),
                0L,
                INTERNAL_NOTIFICATION_SEND_ACTION,
                KyvcEnums.AuditTargetType.NOTIFICATION.name(),
                savedNotification.getNotificationId(),
                INTERNAL_NOTIFICATION_SEND_SUMMARY,
                null
        ));

        return new InternalNotificationSendResponse(
                savedNotification.getNotificationId(),
                INTERNAL_NOTIFICATION_SEND_STATUS
        );
    }

    // 내부 대량 알림 발송 저장
    public InternalNotificationBulkSendResponse sendBulkInternalNotifications(
            InternalNotificationBulkSendRequest request // 내부 대량 알림 발송 요청
    ) {
        validateBulkRequest(request);
        NotificationTemplate template = notificationTemplateRepository.findEnabledByTemplateCode(request.templateCode().trim())
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));
        List<Long> requestedUserIds = request.userIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (requestedUserIds.isEmpty()) {
            throw new ApiException(ErrorCode.NOTIFICATION_BULK_USER_EMPTY);
        }

        List<Long> failedUserIds = requestedUserIds.stream()
                .filter(userId -> userRepository.findById(userId).isEmpty())
                .toList();
        List<Notification> notifications = requestedUserIds.stream()
                .filter(userId -> !failedUserIds.contains(userId))
                .map(userId -> Notification.createFromTemplate(
                        userId,
                        template.getTemplateCode(),
                        renderTemplate(template.getTitleTemplate(), request.payload()),
                        renderTemplate(template.getMessageTemplate(), request.payload()),
                        template.getChannelCode(),
                        request.targetType().trim(),
                        resolveTargetId(request.payload()),
                        template.getTemplateCode()
                ))
                .toList();

        try {
            notificationRepository.saveAll(notifications);
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.INTERNAL_NOTIFICATION_SAVE_FAILED);
        }

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.SYSTEM.name(),
                0L,
                "INTERNAL_NOTIFICATION_BULK_SEND",
                KyvcEnums.AuditTargetType.NOTIFICATION.name(),
                0L,
                "내부 대량 알림 발송 요청 저장",
                null
        ));

        return new InternalNotificationBulkSendResponse(
                requestedUserIds.size(),
                notifications.size(),
                failedUserIds.size(),
                failedUserIds
        );
    }

    // 내부 대량 알림 발송 요청 검증
    private void validateBulkRequest(
            InternalNotificationBulkSendRequest request // 내부 대량 알림 발송 요청
    ) {
        if (request == null || !StringUtils.hasText(request.targetType())) {
            throw new ApiException(ErrorCode.NOTIFICATION_BULK_TARGET_INVALID);
        }
        if (!"USER_IDS".equalsIgnoreCase(request.targetType().trim())) {
            throw new ApiException(ErrorCode.NOTIFICATION_BULK_TARGET_INVALID);
        }
        if (!StringUtils.hasText(request.templateCode())) {
            throw new ApiException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND);
        }
        if (request.userIds() == null || request.userIds().isEmpty()) {
            throw new ApiException(ErrorCode.NOTIFICATION_BULK_USER_EMPTY);
        }
    }

    // 템플릿 문자열 치환
    private String renderTemplate(
            String template, // 템플릿 문자열
            Map<String, Object> payload // 치환 payload
    ) {
        String rendered = template == null ? "" : template;
        if (payload == null || payload.isEmpty()) {
            return rendered;
        }
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}"; // 치환 키
            rendered = rendered.replace(placeholder, String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    // 알림 대상 ID 산정
    private Long resolveTargetId(
            Map<String, Object> payload // 알림 payload
    ) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object kycId = payload.get("kycId");
        if (kycId instanceof Number number) {
            return number.longValue();
        }
        if (kycId instanceof String value && StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
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

    // 내부 알림 발송 요청 검증
    private void validateInternalNotificationRequest(
            InternalNotificationSendRequest request // 내부 알림 발송 요청
    ) {
        if (request == null
                || request.recipientUserId() == null
                || request.recipientUserId() <= 0
                || !StringUtils.hasText(request.type())
                || !StringUtils.hasText(request.title())
                || !StringUtils.hasText(request.message())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 알림 유형 정규화
    private String normalizeNotificationType(
            String notificationType // 원본 알림 유형 코드
    ) {
        String normalizedNotificationType = notificationType.trim().toUpperCase(Locale.ROOT); // 정규화 알림 유형 코드
        try {
            KyvcEnums.NotificationType.valueOf(normalizedNotificationType);
            return normalizedNotificationType;
        } catch (IllegalArgumentException exception) {
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
        int resolvedDefaultPageSize = notificationProperties.resolvedDefaultPageSize(); // 보정 기본 페이지 크기
        int resolvedMaxPageSize = notificationProperties.resolvedMaxPageSize(); // 보정 최대 페이지 크기

        if (size < 1) {
            return resolvedDefaultPageSize;
        }
        return Math.min(size, resolvedMaxPageSize);
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

