package com.kyvc.backendadmin.domain.vp.repository;

import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationDetailResponse;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSearchRequest;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSummaryResponse;
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
import java.util.Optional;

/**
 * {@link VpVerificationQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class VpVerificationQueryRepositoryImpl implements VpVerificationQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminVpVerificationSummaryResponse> search(AdminVpVerificationSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select vp.vp_verification_id,
                       vp.vp_request_id,
                       vp.corporate_id,
                       corporate.corporate_name,
                       vp.credential_id,
                       vp.requester_name,
                       vp.purpose,
                       vp.vp_verification_status_code,
                       vp.replay_suspected_yn,
                       vp.requested_at,
                       vp.presented_at,
                       vp.verified_at,
                       vp.expires_at,
                       vp.callback_status_code
                from vp_verifications vp
                join corporates corporate on corporate.corporate_id = vp.corporate_id
                join credentials credential on credential.credential_id = vp.credential_id
                left join verifiers verifier on verifier.verifier_id = vp.verifier_id
                left join core_requests core_request on core_request.core_request_id = vp.core_request_id
                %s
                order by vp.requested_at desc, vp.vp_verification_id desc
                """.formatted(queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toSummary((Object[]) row))
                .toList();
    }

    @Override
    public long count(AdminVpVerificationSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select count(*)
                from vp_verifications vp
                join corporates corporate on corporate.corporate_id = vp.corporate_id
                join credentials credential on credential.credential_id = vp.credential_id
                left join verifiers verifier on verifier.verifier_id = vp.verifier_id
                left join core_requests core_request on core_request.core_request_id = vp.core_request_id
                %s
                """.formatted(queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<AdminVpVerificationDetailResponse> findDetailById(Long verificationId) {
        Query query = entityManager().createNativeQuery("""
                select vp.vp_verification_id,
                       vp.credential_id,
                       vp.corporate_id,
                       corporate.corporate_name,
                       vp.request_nonce,
                       vp.vp_request_id,
                       vp.purpose,
                       vp.requester_name,
                       vp.required_claims_json,
                       vp.vp_verification_status_code,
                       vp.replay_suspected_yn,
                       vp.result_summary,
                       vp.requested_at,
                       vp.presented_at,
                       vp.verified_at,
                       vp.expires_at,
                       vp.core_request_id,
                       core_request.core_request_status_code,
                       vp.callback_status_code,
                       vp.callback_sent_at,
                       vp.permission_result_json,
                       credential.credential_external_id,
                       credential.credential_type_code,
                       credential.credential_status_code,
                       credential.issuer_did,
                       credential.holder_did,
                       credential.xrpl_tx_hash,
                       credential.wallet_saved_yn,
                       verifier.verifier_id,
                       verifier.verifier_name,
                       verifier.verifier_status_code,
                       verifier.contact_email
                from vp_verifications vp
                join corporates corporate on corporate.corporate_id = vp.corporate_id
                join credentials credential on credential.credential_id = vp.credential_id
                left join verifiers verifier on verifier.verifier_id = vp.verifier_id
                left join core_requests core_request on core_request.core_request_id = vp.core_request_id
                where vp.vp_verification_id = :verificationId
                """);
        query.setParameter("verificationId", verificationId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> toDetail((Object[]) row));
    }

    private QueryParts buildQueryParts(AdminVpVerificationSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(request.status())) {
            where.append(" and vp.vp_verification_status_code = :status");
            parameters.put("status", request.status());
        }
        if (request.corporateId() != null) {
            where.append(" and vp.corporate_id = :corporateId");
            parameters.put("corporateId", request.corporateId());
        }
        if (request.credentialId() != null) {
            where.append(" and vp.credential_id = :credentialId");
            parameters.put("credentialId", request.credentialId());
        }
        if (request.verifierId() != null) {
            where.append(" and vp.verifier_id = :verifierId");
            parameters.put("verifierId", request.verifierId());
        }
        if (StringUtils.hasText(request.requestTypeCode())) {
            where.append(" and vp.request_type_code = :requestTypeCode");
            parameters.put("requestTypeCode", request.requestTypeCode());
        }
        if (StringUtils.hasText(request.replaySuspectedYn())) {
            where.append(" and vp.replay_suspected_yn = :replaySuspectedYn");
            parameters.put("replaySuspectedYn", request.replaySuspectedYn());
        }
        if (StringUtils.hasText(request.testYn())) {
            where.append(" and vp.test_yn = :testYn");
            parameters.put("testYn", request.testYn());
        }
        if (request.fromDate() != null) {
            where.append(" and vp.requested_at >= :fromDate");
            parameters.put("fromDate", request.fromDate().atStartOfDay());
        }
        if (request.toDate() != null) {
            where.append(" and vp.requested_at < :toDateExclusive");
            parameters.put("toDateExclusive", request.toDate().plusDays(1).atStartOfDay());
        }
        if (StringUtils.hasText(request.keyword())) {
            where.append("""
                     and (
                         lower(corporate.corporate_name) like :keyword
                         or lower(coalesce(vp.requester_name, '')) like :keyword
                         or lower(vp.purpose) like :keyword
                         or lower(coalesce(vp.vp_request_id, '')) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        return new QueryParts(where.toString(), parameters);
    }

    private AdminVpVerificationSummaryResponse toSummary(Object[] row) {
        return new AdminVpVerificationSummaryResponse(
                toLong(row[0]),
                toString(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toLong(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toLocalDateTime(row[9]),
                toLocalDateTime(row[10]),
                toLocalDateTime(row[11]),
                toLocalDateTime(row[12]),
                toString(row[13])
        );
    }

    private AdminVpVerificationDetailResponse toDetail(Object[] row) {
        // 민감정보 제외: vp_jwt_hash, challenge, API key/token hash는 조회하지 않는다.
        AdminVpVerificationDetailResponse.CredentialInfo credential = new AdminVpVerificationDetailResponse.CredentialInfo(
                toLong(row[1]),
                toString(row[21]),
                toString(row[22]),
                toString(row[23]),
                toString(row[24]),
                toString(row[25]),
                toString(row[26]),
                toString(row[27])
        );
        AdminVpVerificationDetailResponse.VerifierInfo verifier = new AdminVpVerificationDetailResponse.VerifierInfo(
                toLong(row[28]),
                toString(row[29]),
                toString(row[30]),
                toString(row[31])
        );
        return new AdminVpVerificationDetailResponse(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toString(row[9]),
                toString(row[10]),
                toString(row[11]),
                toLocalDateTime(row[12]),
                toLocalDateTime(row[13]),
                toLocalDateTime(row[14]),
                toLocalDateTime(row[15]),
                toString(row[16]),
                toString(row[17]),
                toString(row[18]),
                toLocalDateTime(row[19]),
                toString(row[20]),
                credential,
                verifier
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

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }

    private record QueryParts(String whereClause, Map<String, Object> parameters) {
    }
}
