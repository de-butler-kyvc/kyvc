package com.kyvc.backendadmin.domain.notification.repository;

import com.kyvc.backendadmin.domain.notification.dto.AdminNotificationTemplateDtos;

import java.util.Optional;

/**
 * 알림 템플릿 변경 Repository입니다.
 */
public interface NotificationTemplateRepository {
    boolean existsByCode(String templateCode);
    Long create(AdminNotificationTemplateDtos.CreateRequest request);
    void update(Long templateId, AdminNotificationTemplateDtos.UpdateRequest request);
    void disable(Long templateId);
    Optional<AdminNotificationTemplateDtos.Response> findById(Long templateId);
}
