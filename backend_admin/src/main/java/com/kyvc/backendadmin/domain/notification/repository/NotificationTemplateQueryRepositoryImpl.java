package com.kyvc.backendadmin.domain.notification.repository;

import com.kyvc.backendadmin.domain.notification.dto.AdminNotificationTemplateDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link NotificationTemplateQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class NotificationTemplateQueryRepositoryImpl implements NotificationTemplateQueryRepository {
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminNotificationTemplateDtos.Response> search(String channelCode, String enabledYn, String keyword, int page, int size) {
        QueryParts parts = where(channelCode, enabledYn, keyword);
        Query query = em().createNativeQuery("""
                select template_id, template_code, template_name, channel_code, title_template, message_template, enabled_yn, created_at, updated_at
                from notification_templates
                %s
                order by created_at desc, template_id desc
                """.formatted(parts.where()));
        bind(query, parts.params());
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream().map(row -> toResponse((Object[]) row)).toList();
    }

    @Override
    public long count(String channelCode, String enabledYn, String keyword) {
        QueryParts parts = where(channelCode, enabledYn, keyword);
        Query query = em().createNativeQuery("select count(*) from notification_templates " + parts.where());
        bind(query, parts.params());
        return ((Number) query.getSingleResult()).longValue();
    }

    private QueryParts where(String channelCode, String enabledYn, String keyword) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(channelCode)) {
            where.append(" and channel_code = :channelCode");
            params.put("channelCode", channelCode);
        }
        if (StringUtils.hasText(enabledYn)) {
            where.append(" and enabled_yn = :enabledYn");
            params.put("enabledYn", enabledYn);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" and (lower(template_code) like :keyword or lower(template_name) like :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        return new QueryParts(where.toString(), params);
    }

    private AdminNotificationTemplateDtos.Response toResponse(Object[] row) {
        return new AdminNotificationTemplateDtos.Response(toLong(row[0]), str(row[1]), str(row[2]), str(row[3]), str(row[4]), str(row[5]), str(row[6]), dt(row[7]), dt(row[8]));
    }
    private void bind(Query q, Map<String, Object> p) { p.forEach(q::setParameter); }
    private Long toLong(Object v) { return v == null ? null : ((Number) v).longValue(); }
    private String str(Object v) { return v == null ? null : v.toString(); }
    private LocalDateTime dt(Object v) { return v == null ? null : v instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) v; }
    private EntityManager em() { return entityManagerProvider.getObject(); }
    private record QueryParts(String where, Map<String, Object> params) {}
}
