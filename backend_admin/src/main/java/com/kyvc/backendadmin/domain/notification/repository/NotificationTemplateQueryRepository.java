package com.kyvc.backendadmin.domain.notification.repository;

import com.kyvc.backendadmin.domain.notification.dto.AdminNotificationTemplateDtos;

import java.util.List;

/**
 * 알림 템플릿 목록 조회 QueryRepository입니다.
 */
public interface NotificationTemplateQueryRepository {
    List<AdminNotificationTemplateDtos.Response> search(String channelCode, String enabledYn, String keyword, int page, int size);
    long count(String channelCode, String enabledYn, String keyword);
}
