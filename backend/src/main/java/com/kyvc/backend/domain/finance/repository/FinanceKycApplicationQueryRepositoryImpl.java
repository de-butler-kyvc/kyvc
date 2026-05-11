package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationDetailResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationListResponse;
import com.kyvc.backend.global.util.KyvcEnums;
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
import java.util.Optional;

// 금융사 방문 KYC 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class FinanceKycApplicationQueryRepositoryImpl implements FinanceKycApplicationQueryRepository {

    private final EntityManager entityManager;

    // 금융사 방문 KYC 목록 조회
    @Override
    public List<FinanceKycApplicationListResponse.Item> searchApplications(
            Long staffUserId, // 금융사 직원 사용자 ID
            String status, // KYC 상태 코드
            String keyword, // 검색어
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        QueryParts parts = buildSearchQueryParts(staffUserId, status, keyword);
        Query query = entityManager.createNativeQuery("""
                select kyc.kyc_id,
                       kyc.kyc_status_code,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       kyc.finance_customer_no,
                       kyc.visited_at
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                %s
                order by coalesce(kyc.visited_at, kyc.created_at) desc, kyc.kyc_id desc
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream()
                .map(row -> toListItem((Object[]) row))
                .toList();
    }

    // 금융사 방문 KYC 목록 건수 조회
    @Override
    public long countApplications(
            Long staffUserId, // 금융사 직원 사용자 ID
            String status, // KYC 상태 코드
            String keyword // 검색어
    ) {
        QueryParts parts = buildSearchQueryParts(staffUserId, status, keyword);
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                %s
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    // 금융사 방문 KYC 상세 조회
    @Override
    public Optional<FinanceKycApplicationDetailResponse> findDetail(
            Long staffUserId, // 금융사 직원 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        Query query = entityManager.createNativeQuery("""
                select kyc.kyc_id,
                       kyc.kyc_status_code,
                       kyc.application_channel_code,
                       corporate.corporate_id,
                       corporate.corporate_name
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                where kyc.kyc_id = :kycId
                  and kyc.application_channel_code = :applicationChannelCode
                  and kyc.finance_staff_user_id = :staffUserId
                """);
        query.setParameter("kycId", kycId);
        query.setParameter("applicationChannelCode", KyvcEnums.ApplicationChannel.FINANCE_VISIT.name());
        query.setParameter("staffUserId", staffUserId);
        return query.getResultList().stream()
                .findFirst()
                .map(row -> toDetailResponse((Object[]) row, findDocuments(kycId)));
    }

    // 제출서류 요약 목록 조회
    private List<FinanceKycApplicationDetailResponse.DocumentSummary> findDocuments(
            Long kycId // KYC 신청 ID
    ) {
        Query query = entityManager.createNativeQuery("""
                select document.document_id,
                       document.document_type_code,
                       document.file_name,
                       document.upload_status_code,
                       document.uploaded_at
                from kyc_documents document
                where document.kyc_id = :kycId
                order by document.uploaded_at desc, document.document_id desc
                """);
        query.setParameter("kycId", kycId);
        return query.getResultList().stream()
                .map(row -> toDocumentSummary((Object[]) row))
                .toList();
    }

    // 목록 조회 조건 조립
    private QueryParts buildSearchQueryParts(
            Long staffUserId, // 금융사 직원 사용자 ID
            String status, // KYC 상태 코드
            String keyword // 검색어
    ) {
        StringBuilder where = new StringBuilder("""
                where kyc.application_channel_code = :applicationChannelCode
                  and kyc.finance_staff_user_id = :staffUserId
                """);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("applicationChannelCode", KyvcEnums.ApplicationChannel.FINANCE_VISIT.name());
        parameters.put("staffUserId", staffUserId);
        if (StringUtils.hasText(status)) {
            where.append(" and kyc.kyc_status_code = :status");
            parameters.put("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            where.append("""
                     and (
                         lower(corporate.corporate_name) like :keyword
                         or lower(corporate.business_registration_no) like :keyword
                         or lower(kyc.finance_customer_no) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        return new QueryParts(where.toString(), parameters);
    }

    // 목록 항목 변환
    private FinanceKycApplicationListResponse.Item toListItem(
            Object[] row // 조회 원본 행
    ) {
        return new FinanceKycApplicationListResponse.Item(
                toLong(row[0]),
                toString(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toLocalDateTime(row[5])
        );
    }

    // 상세 응답 변환
    private FinanceKycApplicationDetailResponse toDetailResponse(
            Object[] row, // 조회 원본 행
            List<FinanceKycApplicationDetailResponse.DocumentSummary> documents // 제출서류 요약 목록
    ) {
        return new FinanceKycApplicationDetailResponse(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                new FinanceKycApplicationDetailResponse.CorporateSummary(
                        toLong(row[3]),
                        toString(row[4])
                ),
                documents
        );
    }

    // 제출서류 요약 변환
    private FinanceKycApplicationDetailResponse.DocumentSummary toDocumentSummary(
            Object[] row // 조회 원본 행
    ) {
        return new FinanceKycApplicationDetailResponse.DocumentSummary(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3]),
                toLocalDateTime(row[4])
        );
    }

    // 쿼리 파라미터 바인딩
    private void bind(
            Query query, // JPA Query
            Map<String, Object> parameters // 바인딩 파라미터
    ) {
        parameters.forEach(query::setParameter);
    }

    // Long 변환
    private Long toLong(
            Object value // 원본 값
    ) {
        return value == null ? null : ((Number) value).longValue();
    }

    // 문자열 변환
    private String toString(
            Object value // 원본 값
    ) {
        return value == null ? null : value.toString();
    }

    // 일시 변환
    private LocalDateTime toLocalDateTime(
            Object value // 원본 값
    ) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return ((Timestamp) value).toLocalDateTime();
    }

    private record QueryParts(
            String whereClause, // WHERE 절
            Map<String, Object> parameters // 바인딩 파라미터
    ) {
    }
}
