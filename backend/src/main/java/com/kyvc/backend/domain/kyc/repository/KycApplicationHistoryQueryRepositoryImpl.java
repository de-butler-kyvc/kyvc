package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.domain.kyc.dto.KycApplicationHistoryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 사용자 KYC 신청 이력 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class KycApplicationHistoryQueryRepositoryImpl implements KycApplicationHistoryQueryRepository {

    private static final String LATEST_CREDENTIAL_CTE = """
            with latest_credential as (
                select *
                from (
                    select credential.*,
                           row_number() over (
                               partition by credential.kyc_id
                               order by coalesce(credential.issued_at, credential.updated_at, credential.created_at) desc,
                                        credential.credential_id desc
                           ) as rn
                    from credentials credential
                ) ranked_credential
                where ranked_credential.rn = 1
            )
            """;

    private final EntityManager entityManager;

    // 사용자 소유 법인 KYC 신청 이력 조회
    @Override
    public List<KycApplicationHistoryResponse.Item> search(
            Long userId, // 사용자 ID
            String status, // KYC 상태 코드
            String keyword, // 검색어
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        QueryParts parts = buildQueryParts(userId, status, keyword);
        Query query = entityManager.createNativeQuery("""
                %s
                select kyc.kyc_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       corporate.business_registration_no,
                       kyc.corporate_type_code,
                       kyc.kyc_status_code,
                       kyc.ai_review_status_code,
                       kyc.ai_review_result_code,
                       kyc.submitted_at,
                       kyc.approved_at,
                       kyc.rejected_at,
                       kyc.created_at,
                       kyc.updated_at,
                       latest_credential.credential_id,
                       latest_credential.credential_status_code,
                       latest_credential.issued_at
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                left join latest_credential on latest_credential.kyc_id = kyc.kyc_id
                %s
                order by coalesce(kyc.submitted_at, kyc.created_at) desc, kyc.kyc_id desc
                """.formatted(LATEST_CREDENTIAL_CTE, parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream()
                .map(row -> toItem((Object[]) row))
                .toList();
    }

    // 사용자 소유 법인 KYC 신청 이력 건수 조회
    @Override
    public long count(
            Long userId, // 사용자 ID
            String status, // KYC 상태 코드
            String keyword // 검색어
    ) {
        QueryParts parts = buildQueryParts(userId, status, keyword);
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                %s
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    // 조회 조건 조립
    private QueryParts buildQueryParts(
            Long userId, // 사용자 ID
            String status, // KYC 상태 코드
            String keyword // 검색어
    ) {
        StringBuilder where = new StringBuilder("where corporate.user_id = :userId");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);

        if (StringUtils.hasText(status)) {
            where.append(" and kyc.kyc_status_code = :status");
            parameters.put("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            where.append("""
                     and (
                         lower(corporate.corporate_name) like :keyword
                         or lower(corporate.business_registration_no) like :keyword
                         or lower(kyc.corporate_type_code) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + keyword.trim().toLowerCase() + "%");
        }
        return new QueryParts(where.toString(), parameters);
    }

    // KYC 신청 이력 항목 변환
    private KycApplicationHistoryResponse.Item toItem(Object[] row // KYC 신청 이력 원본 행
    ) {
        return new KycApplicationHistoryResponse.Item(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toLocalDateTime(row[8]),
                toLocalDateTime(row[9]),
                toLocalDateTime(row[10]),
                toLocalDateTime(row[11]),
                toLocalDateTime(row[12]),
                toLong(row[13]),
                toString(row[14]),
                toLocalDateTime(row[15])
        );
    }

    // 파라미터 바인딩
    private void bind(
            Query query, // native query
            Map<String, Object> parameters // 바인딩 파라미터
    ) {
        parameters.forEach(query::setParameter);
    }

    // Long 변환
    private Long toLong(Object value // 원본 값
    ) {
        return value == null ? null : ((Number) value).longValue();
    }

    // 문자열 변환
    private String toString(Object value // 원본 값
    ) {
        return value == null ? null : value.toString();
    }

    // LocalDateTime 변환
    private LocalDateTime toLocalDateTime(Object value // 원본 값
    ) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private record QueryParts(
            String whereClause,
            Map<String, Object> parameters
    ) {
    }
}
