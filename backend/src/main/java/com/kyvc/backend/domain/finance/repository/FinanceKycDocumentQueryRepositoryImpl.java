package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

// 금융사 방문 KYC 문서 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class FinanceKycDocumentQueryRepositoryImpl implements FinanceKycDocumentQueryRepository {

    private final EntityManager entityManager;

    // KYC 신청 ID 기준 문서 목록 조회
    @Override
    public List<FinanceKycDocumentListResponse.Item> findDocuments(
            Long kycId // KYC 신청 ID
    ) {
        Query query = entityManager.createNativeQuery("""
                select document.document_id,
                       document.kyc_id,
                       document.document_type_code,
                       document.file_name,
                       document.file_size,
                       document.upload_status_code,
                       document.uploaded_by_type_code,
                       document.uploaded_at
                from kyc_documents document
                where document.kyc_id = :kycId
                order by document.uploaded_at desc, document.document_id desc
                """);
        query.setParameter("kycId", kycId);
        return query.getResultList().stream()
                .map(row -> toItem((Object[]) row))
                .toList();
    }

    // 문서 목록 항목 변환
    private FinanceKycDocumentListResponse.Item toItem(
            Object[] row // 조회 원본 행
    ) {
        return new FinanceKycDocumentListResponse.Item(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toLong(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLocalDateTime(row[7])
        );
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
}
