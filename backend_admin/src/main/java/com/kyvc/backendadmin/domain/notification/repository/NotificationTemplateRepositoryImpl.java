package com.kyvc.backendadmin.domain.notification.repository;

import com.kyvc.backendadmin.domain.notification.dto.AdminNotificationTemplateDtos;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@link NotificationTemplateRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationTemplateRepositoryImpl implements NotificationTemplateRepository {
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public boolean existsByCode(String templateCode) {
        Number count = (Number) em().createNativeQuery("select count(*) from notification_templates where template_code = :code")
                .setParameter("code", templateCode).getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public Long create(AdminNotificationTemplateDtos.CreateRequest request) {
        Object id = em().createNativeQuery("""
                insert into notification_templates (template_code, template_name, channel_code, title_template, message_template, enabled_yn, created_at, updated_at)
                values (:code, :name, :channel, :title, :message, :enabled, now(), now())
                returning template_id
                """)
                .setParameter("code", request.templateCode())
                .setParameter("name", request.templateName())
                .setParameter("channel", request.channelCode())
                .setParameter("title", request.titleTemplate())
                .setParameter("message", request.messageTemplate())
                .setParameter("enabled", request.enabledYn() == null ? "Y" : request.enabledYn())
                .getSingleResult();
        return ((Number) id).longValue();
    }

    @Override
    public void update(Long templateId, AdminNotificationTemplateDtos.UpdateRequest request) {
        em().createNativeQuery("""
                update notification_templates
                set template_name = coalesce(:name, template_name),
                    channel_code = coalesce(:channel, channel_code),
                    title_template = coalesce(:title, title_template),
                    message_template = coalesce(:message, message_template),
                    enabled_yn = coalesce(:enabled, enabled_yn),
                    updated_at = now()
                where template_id = :id
                """)
                .setParameter("name", request.templateName())
                .setParameter("channel", request.channelCode())
                .setParameter("title", request.titleTemplate())
                .setParameter("message", request.messageTemplate())
                .setParameter("enabled", request.enabledYn())
                .setParameter("id", templateId)
                .executeUpdate();
    }

    @Override
    public void disable(Long templateId) {
        em().createNativeQuery("update notification_templates set enabled_yn = 'N', updated_at = now() where template_id = :id")
                .setParameter("id", templateId).executeUpdate();
    }

    @Override
    public Optional<AdminNotificationTemplateDtos.Response> findById(Long templateId) {
        List<?> rows = em().createNativeQuery("""
                select template_id, template_code, template_name, channel_code, title_template, message_template, enabled_yn, created_at, updated_at
                from notification_templates where template_id = :id
                """).setParameter("id", templateId).getResultList();
        return rows.stream().findFirst().map(row -> toResponse((Object[]) row));
    }

    private AdminNotificationTemplateDtos.Response toResponse(Object[] row) {
        return new AdminNotificationTemplateDtos.Response(toLong(row[0]), str(row[1]), str(row[2]), str(row[3]), str(row[4]), str(row[5]), str(row[6]), dt(row[7]), dt(row[8]));
    }
    private Long toLong(Object v) { return v == null ? null : ((Number) v).longValue(); }
    private String str(Object v) { return v == null ? null : v.toString(); }
    private LocalDateTime dt(Object v) { return v instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) v; }
    private EntityManager em() { return entityManagerProvider.getObject(); }
}
