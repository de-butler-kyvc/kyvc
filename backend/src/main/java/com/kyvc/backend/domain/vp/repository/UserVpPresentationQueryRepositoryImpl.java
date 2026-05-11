package com.kyvc.backend.domain.vp.repository;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

// 사용자 VP 제출 이력 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class UserVpPresentationQueryRepositoryImpl implements UserVpPresentationQueryRepository {

    private final EntityManager entityManager;

    // 사용자 소유 VP 제출 이력 목록 조회
    @Override
    public List<VpVerification> findByUserId(
            Long userId, // 사용자 ID
            KyvcEnums.VpVerificationStatus status, // 검증 상태
            String verifierName, // Verifier명
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        TypedQuery<VpVerification> query = createBaseQuery(userId, status, verifierName, false, VpVerification.class);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    // 사용자 소유 VP 제출 이력 건수 조회
    @Override
    public long countByUserId(
            Long userId, // 사용자 ID
            KyvcEnums.VpVerificationStatus status, // 검증 상태
            String verifierName // Verifier명
    ) {
        return createBaseQuery(userId, status, verifierName, true, Long.class)
                .getSingleResult();
    }

    private <T> TypedQuery<T> createBaseQuery(
            Long userId, // 사용자 ID
            KyvcEnums.VpVerificationStatus status, // 검증 상태
            String verifierName, // Verifier명
            boolean countQuery, // 건수 조회 여부
            Class<T> resultType // 결과 타입
    ) {
        StringBuilder jpql = new StringBuilder(countQuery ? """
                select count(vp)
                from VpVerification vp, Credential c, Corporate corp
                where vp.credentialId = c.credentialId
                  and c.corporateId = corp.corporateId
                  and corp.userId = :userId
                  and vp.presentedAt is not null
                """ : """
                select vp
                from VpVerification vp, Credential c, Corporate corp
                where vp.credentialId = c.credentialId
                  and c.corporateId = corp.corporateId
                  and corp.userId = :userId
                  and vp.presentedAt is not null
                """);
        if (status != null) {
            jpql.append(" and vp.vpVerificationStatus = :status");
        }
        if (StringUtils.hasText(verifierName)) {
            jpql.append(" and lower(vp.requesterName) like lower(:verifierName)");
        }
        if (!countQuery) {
            jpql.append(" order by vp.presentedAt desc, vp.vpVerificationId desc");
        }

        TypedQuery<T> query = entityManager.createQuery(jpql.toString(), resultType);
        query.setParameter("userId", userId);
        if (status != null) {
            query.setParameter("status", status);
        }
        if (StringUtils.hasText(verifierName)) {
            query.setParameter("verifierName", "%" + verifierName.trim() + "%");
        }
        return query;
    }
}
