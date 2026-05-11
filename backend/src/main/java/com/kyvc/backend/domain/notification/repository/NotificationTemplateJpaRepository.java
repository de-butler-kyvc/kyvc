package com.kyvc.backend.domain.notification.repository;

import com.kyvc.backend.domain.notification.domain.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 알림 템플릿 JPA Repository
 */
public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * 템플릿 코드와 사용 여부 기준 조회
     *
     * @param templateCode 템플릿 코드
     * @param enabledYn 사용 여부
     * @return 템플릿 조회 결과
     */
    Optional<NotificationTemplate> findByTemplateCodeAndEnabledYn(
            String templateCode, // 템플릿 코드
            String enabledYn // 사용 여부
    );
}
