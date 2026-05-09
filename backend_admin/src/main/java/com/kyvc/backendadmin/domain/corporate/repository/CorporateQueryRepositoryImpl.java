package com.kyvc.backendadmin.domain.corporate.repository;

import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserListResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link CorporateQueryRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>users, corporates, kyc_applications 최신 1건을 native SQL로 조인하여
 * 관리자 법인 사용자 관리용 읽기 모델을 생성합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class CorporateQueryRepositoryImpl implements CorporateQueryRepository {

    private static final String LATEST_KYC_CTE = """
            with latest_kyc as (
                select *
                from (
                    select kyc.*,
                           row_number() over (
                               partition by kyc.corporate_id
                               order by coalesce(kyc.submitted_at, kyc.updated_at, kyc.created_at) desc,
                                        kyc.kyc_id desc
                           ) as rn
                    from kyc_applications kyc
                ) ranked_kyc
                where ranked_kyc.rn = 1
            )
            """;

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminCorporateUserListResponse.Item> searchUsers(AdminCorporateUserSearchRequest request) {
        QueryParts queryParts = buildListQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                %s
                select u.user_id,
                       u.email,
                       u.user_name,
                       u.phone,
                       u.notification_enabled_yn,
                       u.mfa_enabled_yn,
                       u.mfa_type_code,
                       u.last_password_changed_at,
                       u.onboarding_corporate_name,
                       u.user_status_code,
                       c.corporate_id,
                       c.corporate_name,
                       c.corporate_status_code,
                       lk.kyc_id,
                       lk.kyc_status_code,
                       u.created_at
                from users u
                left join corporates c on c.user_id = u.user_id
                left join latest_kyc lk on lk.corporate_id = c.corporate_id
                %s
                order by u.created_at desc, u.user_id desc
                """.formatted(LATEST_KYC_CTE, queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        List<?> rows = query.getResultList();
        return rows
                .stream()
                .map(row -> toListItem((Object[]) row))
                .toList();
    }

    @Override
    public long countUsers(AdminCorporateUserSearchRequest request) {
        QueryParts queryParts = buildListQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                %s
                select count(distinct u.user_id)
                from users u
                left join corporates c on c.user_id = u.user_id
                left join latest_kyc lk on lk.corporate_id = c.corporate_id
                %s
                """.formatted(LATEST_KYC_CTE, queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<AdminCorporateUserDetailResponse> findUserDetail(Long userId) {
        Query query = entityManager().createNativeQuery("""
                %s
                select u.user_id,
                       u.email,
                       u.user_name,
                       u.phone,
                       u.notification_enabled_yn,
                       u.mfa_enabled_yn,
                       u.mfa_type_code,
                       u.last_password_changed_at,
                       u.onboarding_corporate_name,
                       u.user_type_code,
                       u.user_status_code,
                       u.created_at,
                       u.updated_at,
                       c.corporate_id,
                       c.corporate_name,
                       c.business_registration_no,
                       c.representative_name,
                       c.corporate_status_code,
                       lk.kyc_id,
                       lk.kyc_status_code,
                       lk.corporate_type_code,
                       lk.submitted_at,
                       lk.approved_at,
                       lk.rejected_at
                from users u
                left join corporates c on c.user_id = u.user_id
                left join latest_kyc lk on lk.corporate_id = c.corporate_id
                where u.user_id = :userId
                  and u.user_type_code = 'CORPORATE_USER'
                """.formatted(LATEST_KYC_CTE));
        query.setParameter("userId", userId);
        List<?> rows = query.getResultList();
        return rows
                .stream()
                .findFirst()
                .map(row -> toUserDetail((Object[]) row));
    }

    @Override
    public Optional<AdminCorporateDetailResponse> findCorporateDetail(Long corporateId) {
        Query query = entityManager().createNativeQuery("""
                %s
                select c.corporate_id,
                       c.user_id,
                       u.email,
                       u.user_status_code,
                       c.corporate_name,
                       c.corporate_phone,
                       c.corporate_type_code,
                       c.established_date,
                       c.website,
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
                       lk.kyc_id,
                       lk.kyc_status_code,
                       lk.corporate_type_code,
                       lk.submitted_at,
                       lk.approved_at,
                       lk.rejected_at,
                       c.created_at,
                       c.updated_at
                from corporates c
                join users u on u.user_id = c.user_id
                left join latest_kyc lk on lk.corporate_id = c.corporate_id
                where c.corporate_id = :corporateId
                """.formatted(LATEST_KYC_CTE));
        query.setParameter("corporateId", corporateId);
        List<?> rows = query.getResultList();
        return rows
                .stream()
                .findFirst()
                .map(row -> toCorporateDetail((Object[]) row));
    }

    private QueryParts buildListQueryParts(AdminCorporateUserSearchRequest request) {
        StringBuilder where = new StringBuilder("where u.user_type_code = 'CORPORATE_USER'");
        Map<String, Object> parameters = new HashMap<>();

        if (StringUtils.hasText(request.keyword())) {
            where.append("""
                     and (
                         lower(u.email) like :keyword
                         or lower(c.corporate_name) like :keyword
                         or lower(c.business_registration_no) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.status())) {
            where.append(" and u.user_status_code = :status");
            parameters.put("status", request.status());
        }
        if (StringUtils.hasText(request.corporateName())) {
            where.append(" and lower(c.corporate_name) like :corporateName");
            parameters.put("corporateName", "%" + request.corporateName().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.kycStatus())) {
            where.append(" and lk.kyc_status_code = :kycStatus");
            parameters.put("kycStatus", request.kycStatus());
        }

        return new QueryParts(where.toString(), parameters);
    }

    private AdminCorporateUserListResponse.Item toListItem(Object[] row) {
        return new AdminCorporateUserListResponse.Item(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLocalDateTime(row[7]),
                toString(row[8]),
                toString(row[9]),
                toLong(row[10]),
                toString(row[11]),
                toString(row[12]),
                toLong(row[13]),
                toString(row[14]),
                toLocalDateTime(row[15])
        );
    }

    private AdminCorporateUserDetailResponse toUserDetail(Object[] row) {
        AdminCorporateUserDetailResponse.CorporateInfo corporate = row[13] == null
                ? null
                : new AdminCorporateUserDetailResponse.CorporateInfo(
                toLong(row[13]),
                toString(row[14]),
                toString(row[15]),
                toString(row[16]),
                toString(row[17])
        );
        return new AdminCorporateUserDetailResponse(
                new AdminCorporateUserDetailResponse.UserInfo(
                        toLong(row[0]),
                        toString(row[1]),
                        toString(row[2]),
                        toString(row[3]),
                        toString(row[4]),
                        toString(row[5]),
                        toString(row[6]),
                        toLocalDateTime(row[7]),
                        toString(row[8]),
                        toString(row[9]),
                        toString(row[10]),
                        toLocalDateTime(row[11]),
                        toLocalDateTime(row[12])
                ),
                corporate,
                toKycInfo(row, 18)
        );
    }

    private AdminCorporateDetailResponse toCorporateDetail(Object[] row) {
        return new AdminCorporateDetailResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLocalDate(row[7]),
                toString(row[8]),
                toString(row[9]),
                toString(row[10]),
                toString(row[11]),
                toString(row[12]),
                toString(row[13]),
                toString(row[14]),
                toString(row[15]),
                toString(row[16]),
                toString(row[17]),
                toString(row[18]),
                toString(row[19]),
                toString(row[20]),
                toKycInfo(row, 21),
                toLocalDateTime(row[27]),
                toLocalDateTime(row[28])
        );
    }

    private AdminCorporateUserDetailResponse.KycInfo toKycInfo(Object[] row, int offset) {
        if (row[offset] == null) {
            return null;
        }
        return new AdminCorporateUserDetailResponse.KycInfo(
                toLong(row[offset]),
                toString(row[offset + 1]),
                toString(row[offset + 2]),
                toLocalDateTime(row[offset + 3]),
                toLocalDateTime(row[offset + 4]),
                toLocalDateTime(row[offset + 5])
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
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return (LocalDate) value;
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
