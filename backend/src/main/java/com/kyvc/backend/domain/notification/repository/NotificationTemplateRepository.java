package com.kyvc.backend.domain.notification.repository;

import com.kyvc.backend.domain.notification.domain.NotificationTemplate;

import java.util.Optional;

/**
 * 알림 템플릿 Repository
 */
public interface NotificationTemplateRepository {

    /**
     * 활성 템플릿 조회
     *
     * @param templateCode 템플릿 코드
     * @return 템플릿 조회 결과
     */
    Optional<NotificationTemplate> findEnabledByTemplateCode(
            String templateCode // 템플릿 코드
    );
}
