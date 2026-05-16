package com.kyvc.backend.domain.kyc.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

// KYC AI 심사 결과 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class KycAiReviewQueryRepositoryImpl implements KycAiReviewQueryRepository {

    private final EntityManager entityManager;

    // KYC AI 심사 결과 조회
    @Override
    public Optional<Row> findByKycId(
            Long kycId // KYC 신청 ID
    ) {
        Query query = entityManager.createNativeQuery("""
                select kyc.kyc_id,
                       kyc.corporate_id,
                       corporate.user_id as corporate_user_id,
                       kyc.applicant_user_id,
                       kyc.kyc_status_code,
                       kyc.ai_review_status_code,
                       kyc.ai_review_result_code,
                       kyc.ai_confidence_score,
                       kyc.ai_review_summary,
                       kyc.ai_review_detail_json,
                       kyc.core_ai_assessment_json,
                       kyc.manual_review_reason,
                       kyc.ai_review_reason_code,
                       kyc.approved_at,
                       kyc.rejected_at,
                       kyc.updated_at
                from kyc_applications kyc
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                where kyc.kyc_id = :kycId
                """);
        query.setParameter("kycId", kycId);
        try {
            return Optional.of(toRow((Object[]) query.getSingleResult()));
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }

    // 조회 행 변환
    private Row toRow(
            Object[] row // 조회 원본 행
    ) {
        return new Row(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toBigDecimal(row[7]),
                toString(row[8]),
                toString(row[9]),
                toString(row[10]),
                toString(row[11]),
                toString(row[12]),
                toLocalDateTime(row[13]),
                toLocalDateTime(row[14]),
                toLocalDateTime(row[15])
        );
    }

    // Long 변환
    private Long toLong(
            Object value // 원본 값
    ) {
        return value == null ? null : ((Number) value).longValue();
    }

    // BigDecimal 변환
    private BigDecimal toBigDecimal(
            Object value // 원본 값
    ) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        return new BigDecimal(value.toString());
    }

    // 문자열 변환
    private String toString(
            Object value // 원본 값
    ) {
        return value == null ? null : value.toString();
    }

    // LocalDateTime 변환
    private LocalDateTime toLocalDateTime(
            Object value // 원본 값
    ) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
