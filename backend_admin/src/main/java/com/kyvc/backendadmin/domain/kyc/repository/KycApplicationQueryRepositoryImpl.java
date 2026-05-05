package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationCorporateResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationListResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link KycApplicationQueryRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>kyc_applications를 중심으로 corporates, users, 최신 kyc_supplements 1건을
 * native SQL로 조인하여 관리자 KYC 신청 조회용 읽기 모델을 생성합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class KycApplicationQueryRepositoryImpl implements KycApplicationQueryRepository {

    private static final String LATEST_SUPPLEMENT_CTE = """
            with latest_supplement as (
                select *
                from (
                    select supplement.*,
                           row_number() over (
                               partition by supplement.kyc_id
                               order by coalesce(supplement.requested_at, supplement.completed_at) desc,
                                        supplement.supplement_id desc
                           ) as rn
                    from kyc_supplements supplement
                ) ranked_supplement
                where ranked_supplement.rn = 1
            )
            """;

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminKycApplicationListResponse.Item> search(AdminKycApplicationSearchRequest request) {
        QueryParts queryParts = buildSearchQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                %s
                select kyc.kyc_id,
                       kyc.corporate_id,
                       c.corporate_name,
                       c.business_registration_no,
                       kyc.applicant_user_id,
                       u.email,
                       kyc.kyc_status_code,
                       kyc.ai_review_status_code,
                       kyc.ai_review_result_code,
                       kyc.ai_confidence_score,
                       case when ls.supplement_id is null then 'N' else 'Y' end as supplement_yn,
                       ls.supplement_status_code,
                       kyc.submitted_at,
                       kyc.created_at,
                       kyc.updated_at
                from kyc_applications kyc
                join corporates c on c.corporate_id = kyc.corporate_id
                join users u on u.user_id = kyc.applicant_user_id
                left join latest_supplement ls on ls.kyc_id = kyc.kyc_id
                %s
                order by coalesce(kyc.submitted_at, kyc.created_at) desc, kyc.kyc_id desc
                """.formatted(LATEST_SUPPLEMENT_CTE, queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toListItem((Object[]) row))
                .toList();
    }

    @Override
    public long count(AdminKycApplicationSearchRequest request) {
        QueryParts queryParts = buildSearchQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                %s
                select count(distinct kyc.kyc_id)
                from kyc_applications kyc
                join corporates c on c.corporate_id = kyc.corporate_id
                join users u on u.user_id = kyc.applicant_user_id
                left join latest_supplement ls on ls.kyc_id = kyc.kyc_id
                %s
                """.formatted(LATEST_SUPPLEMENT_CTE, queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<AdminKycApplicationCorporateResponse> findCorporateByKycId(Long kycId) {
        Query query = entityManager().createNativeQuery("""
                select kyc.kyc_id,
                       kyc.kyc_status_code,
                       kyc.corporate_type_code,
                       c.corporate_id,
                       c.corporate_name,
                       c.business_registration_no,
                       c.corporate_registration_no,
                       c.representative_name,
                       c.representative_phone,
                       c.representative_email,
                       c.agent_name,
                       c.agent_phone,
                       c.agent_email,
                       c.agent_authority_scope,
                       c.address,
                       c.business_type,
                       c.corporate_status_code,
                       kyc.applicant_user_id,
                       u.email,
                       kyc.submitted_at
                from kyc_applications kyc
                join corporates c on c.corporate_id = kyc.corporate_id
                join users u on u.user_id = kyc.applicant_user_id
                where kyc.kyc_id = :kycId
                """);
        query.setParameter("kycId", kycId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> toCorporateResponse((Object[]) row));
    }

    private QueryParts buildSearchQueryParts(AdminKycApplicationSearchRequest request) {
        // 검색 조건 조립: null이 아닌 조건만 native SQL where 절과 바인딩 파라미터에 추가한다.
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        if (StringUtils.hasText(request.status())) {
            where.append(" and kyc.kyc_status_code = :status");
            parameters.put("status", request.status());
        }
        if (StringUtils.hasText(request.keyword())) {
            where.append("""
                     and (
                         lower(c.corporate_name) like :keyword
                         or lower(c.business_registration_no) like :keyword
                         or lower(u.email) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (request.submittedFrom() != null) {
            where.append(" and kyc.submitted_at >= :submittedFrom");
            parameters.put("submittedFrom", request.submittedFrom().atStartOfDay());
        }
        if (request.submittedTo() != null) {
            where.append(" and kyc.submitted_at < :submittedToExclusive");
            parameters.put("submittedToExclusive", request.submittedTo().plusDays(1).atStartOfDay());
        }
        if (StringUtils.hasText(request.aiReviewStatus())) {
            where.append(" and kyc.ai_review_status_code = :aiReviewStatus");
            parameters.put("aiReviewStatus", request.aiReviewStatus());
        }
        if (StringUtils.hasText(request.supplementYn())) {
            // supplementYn 계산: 최신 보완요청 행 존재 여부를 Y/N 조건으로 변환한다.
            if ("Y".equals(request.supplementYn())) {
                where.append(" and ls.supplement_id is not null");
            } else {
                where.append(" and ls.supplement_id is null");
            }
        }

        return new QueryParts(where.toString(), parameters);
    }

    private AdminKycApplicationListResponse.Item toListItem(Object[] row) {
        // 최근 상태 조회: 최신 kyc_supplements 1건의 상태를 목록 응답에 함께 노출한다.
        return new AdminKycApplicationListResponse.Item(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toLong(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toBigDecimal(row[9]),
                toString(row[10]),
                toString(row[11]),
                toLocalDateTime(row[12]),
                toLocalDateTime(row[13]),
                toLocalDateTime(row[14])
        );
    }

    private AdminKycApplicationCorporateResponse toCorporateResponse(Object[] row) {
        return new AdminKycApplicationCorporateResponse(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toLong(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toString(row[9]),
                toString(row[10]),
                toString(row[11]),
                toString(row[12]),
                toString(row[13]),
                toString(row[14]),
                toString(row[15]),
                toString(row[16]),
                toLong(row[17]),
                toString(row[18]),
                toLocalDateTime(row[19])
        );
    }

    private void bindParameters(Query query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
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

    private record QueryParts(
            String whereClause,
            Map<String, Object> parameters
    ) {
    }
}
