package com.kyvc.backendadmin.domain.notification.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.notification.dto.AdminNotificationTemplateDtos;
import com.kyvc.backendadmin.domain.notification.repository.NotificationTemplateQueryRepository;
import com.kyvc.backendadmin.domain.notification.repository.NotificationTemplateRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 알림 템플릿 관리 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationTemplateService {
    private static final Set<String> CHANNELS = Set.of("EMAIL", "SMS", "PUSH", "IN_APP");
    private final NotificationTemplateRepository repository;
    private final NotificationTemplateQueryRepository queryRepository;
    private final AuditLogWriter auditLogWriter;

    /** 알림 템플릿 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public AdminNotificationTemplateDtos.PageResponse search(String channelCode, String enabledYn, String keyword, Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? 15 : Math.min(size, 100);
        long total = queryRepository.count(channelCode, enabledYn, keyword);
        return new AdminNotificationTemplateDtos.PageResponse(queryRepository.search(channelCode, enabledYn, keyword, p, s), p, s, total, total == 0 ? 0 : (int) Math.ceil((double) total / s));
    }

    /** 템플릿 상세를 조회합니다. */
    @Transactional(readOnly = true)
    public AdminNotificationTemplateDtos.Response get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));
    }

    /** 템플릿을 등록합니다. */
    @Transactional
    public AdminNotificationTemplateDtos.Response create(AdminNotificationTemplateDtos.CreateRequest request) {
        validateChannel(request.channelCode());
        if (repository.existsByCode(request.templateCode())) {
            throw new ApiException(ErrorCode.NOTIFICATION_TEMPLATE_ALREADY_EXISTS);
        }
        Long id = repository.create(request);
        audit("NOTIFICATION_TEMPLATE_CREATED", id, request.templateCode());
        return get(id);
    }

    /** 템플릿을 수정합니다. */
    @Transactional
    public AdminNotificationTemplateDtos.Response update(Long id, AdminNotificationTemplateDtos.UpdateRequest request) {
        get(id);
        if (request.channelCode() != null) validateChannel(request.channelCode());
        repository.update(id, request);
        audit("NOTIFICATION_TEMPLATE_UPDATED", id, null);
        return get(id);
    }

    /** 템플릿을 비활성화합니다. */
    @Transactional
    public void delete(Long id) {
        get(id);
        repository.disable(id);
        audit("NOTIFICATION_TEMPLATE_DISABLED", id, null);
    }

    private void validateChannel(String channelCode) {
        if (!CHANNELS.contains(channelCode)) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "channelCode가 유효하지 않습니다.");
        }
    }

    private void audit(String action, Long id, String value) {
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), action, KyvcEnums.AuditTargetType.NOTIFICATION_TEMPLATE, id, action, null, value);
    }
}
