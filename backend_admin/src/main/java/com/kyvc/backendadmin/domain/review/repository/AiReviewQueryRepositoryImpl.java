package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.review.dto.AdminAiReviewDetailResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewAgentAuthorityResponse;
import com.kyvc.backendadmin.domain.review.dto.KycReviewHistoryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@link AiReviewQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class AiReviewQueryRepositoryImpl implements AiReviewQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<AdminAiReviewDetailResponse> findAiReviewDetail(Long kycId) {
        Query query = entityManager().createNativeQuery("""
                select kyc.kyc_id,
                       kyc.ai_review_status_code,
                       kyc.ai_review_result_code,
                       kyc.ai_confidence_score,
                       kyc.ai_review_summary,
                       kyc.ai_review_detail_json,
                       kyc.core_ai_assessment_json,
                       kyc.manual_review_reason,
                       kyc.ai_review_reason_code,
                       kyc.updated_at
                from kyc_applications kyc
                where kyc.kyc_id = :kycId
                """);
        query.setParameter("kycId", kycId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> toAiReviewDetail((Object[]) row));
    }

    @Override
    public Optional<AiReviewAgentAuthorityResponse.AgentAuthority> findAgentAuthorityBase(Long kycId) {
        Query query = entityManager().createNativeQuery("""
                select corporate.agent_name,
                       corporate.agent_authority_scope
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                where kyc.kyc_id = :kycId
                """);
        query.setParameter("kycId", kycId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> toAgentAuthorityBase((Object[]) row));
    }

    @Override
    public List<KycReviewHistoryResponse> findReviewHistories(Long kycId) {
        Query query = entityManager().createNativeQuery("""
                select review_history.review_history_id,
                       review_history.kyc_id,
                       review_history.review_action_type_code,
                       review_history.before_kyc_status_code,
                       review_history.after_kyc_status_code,
                       review_history.admin_id,
                       review_history.comment,
                       review_history.created_at
                from kyc_review_histories review_history
                where review_history.kyc_id = :kycId
                order by review_history.created_at desc, review_history.review_history_id desc
                """);
        query.setParameter("kycId", kycId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toReviewHistory((Object[]) row))
                .toList();
    }

    private AdminAiReviewDetailResponse toAiReviewDetail(Object[] row) {
        return new AdminAiReviewDetailResponse(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toBigDecimal(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toLocalDateTime(row[9])
        );
    }

    private AiReviewAgentAuthorityResponse.AgentAuthority toAgentAuthorityBase(Object[] row) {
        return new AiReviewAgentAuthorityResponse.AgentAuthority(
                toString(row[0]),
                toString(row[1]),
                null,
                null,
                null,
                null,
                null
        );
    }

    private KycReviewHistoryResponse toReviewHistory(Object[] row) {
        Long reviewerId = toLong(row[5]);
        String reviewerType = reviewerId == null ? "SYSTEM" : "ADMIN";
        return new KycReviewHistoryResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                null,
                null,
                toString(row[6]),
                reviewerType,
                reviewerId,
                toLocalDateTime(row[7])
        );
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? null : (BigDecimal) value;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
