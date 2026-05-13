package com.kyvc.backend.domain.notification.repository;

import com.kyvc.backend.domain.notification.domain.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 알림 템플릿 Repository 구현체
@Repository
@RequiredArgsConstructor
public class NotificationTemplateRepositoryImpl implements NotificationTemplateRepository {

    private static final String ENABLED_YN = "Y"; // 사용 여부 Y

    private final NotificationTemplateJpaRepository notificationTemplateJpaRepository;

    @Override
    public Optional<NotificationTemplate> findEnabledByTemplateCode(
            String templateCode // 템플릿 코드
    ) {
        return notificationTemplateJpaRepository.findByTemplateCodeAndEnabledYn(templateCode, ENABLED_YN);
    }
}
