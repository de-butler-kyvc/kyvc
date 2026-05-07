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
        // 최근 Core 요청 조회: KYC 신청을 대상으로 한 AI_REVIEW 요청 중 가장 최근 요청을 가져온다.
        Query query = entityManager().createNativeQuery("""
                with latest_ai_core_request as (
                    select *
                    from (
                        select core_request.*,
                               row_number() over (
                                   partition by core_request.target_id
                                   order by coalesce(core_request.requested_at, core_request.created_at) desc,
                                            core_request.core_request_id desc
                               ) as rn
                        from core_requests core_request
                        where core_request.core_target_type_code = 'KYC_APPLICATION'
                          and core_request.core_request_type_code = 'AI_REVIEW'
                    ) ranked_core_request
                    where ranked_core_request.rn = 1
                ),
                latest_ai_history as (
                    select *
                    from (
                        select review_history.*,
                               row_number() over (
                                   partition by review_history.kyc_id
                                   order by review_history.created_at desc,
                                            review_history.review_history_id desc
                               ) as rn
                        from kyc_review_histories review_history
                        where review_history.review_action_type_code in ('AI_COMPLETE', 'AI_FAILED')
                    ) ranked_review_history
                    where ranked_review_history.rn = 1
                )
                select kyc.kyc_id,
                       kyc.ai_review_status_code,
                       kyc.ai_review_result_code,
                       kyc.ai_confidence_score,
                       kyc.manual_review_reason,
                       latest_ai_core_request.core_request_id,
                       latest_ai_core_request.core_request_status_code,
                       coalesce(latest_ai_history.created_at, latest_ai_core_request.completed_at) as reviewed_at
                from kyc_applications kyc
                left join latest_ai_core_request on latest_ai_core_request.target_id = kyc.kyc_id
                left join latest_ai_history on latest_ai_history.kyc_id = kyc.kyc_id
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
                       review_history.review_action_type_code,
                       review_history.admin_id,
                       admin_user.name,
                       review_history.comment,
                       review_history.created_at
                from kyc_review_histories review_history
                left join admin_users admin_user on admin_user.admin_id = review_history.admin_id
                where review_history.kyc_id = :kycId
                  and review_history.review_action_type_code in (
                      'AI_START',
                      'AI_COMPLETE',
                      'AI_FAILED',
                      'REQUEST_AI_REVIEW',
                      'MANUAL_REVIEW',
                      'APPROVE',
                      'REJECT',
                      'REQUEST_SUPPLEMENT',
                      'SUPPLEMENT_SUBMIT',
                      'ISSUE_VC',
                      'CHANGE_STATUS'
                  )
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
                toLocalDateTime(row[7])
        );
    }

    private AiReviewAgentAuthorityResponse.AgentAuthority toAgentAuthorityBase(Object[] row) {
        return new AiReviewAgentAuthorityResponse.AgentAuthority(
                toString(row[0]),
                null,
                null,
                null,
                toString(row[1]),
                null,
                null
        );
    }

    private KycReviewHistoryResponse toReviewHistory(Object[] row) {
        Long actorId = toLong(row[2]);
        // actorType에 따라 actorName을 결정한다. 현재 kyc_review_histories는 admin_id만 보관하므로 없으면 SYSTEM으로 보정한다.
        String actorType = actorId == null ? "SYSTEM" : "ADMIN";
        String actorName = actorId == null ? "SYSTEM" : toString(row[3]);
        return new KycReviewHistoryResponse(
                toLong(row[0]),
                toString(row[1]),
                actorType,
                actorId,
                actorName,
                toString(row[4]),
                toLocalDateTime(row[5])
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
