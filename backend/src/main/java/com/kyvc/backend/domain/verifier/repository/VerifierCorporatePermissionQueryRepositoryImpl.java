package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.verifier.dto.VerifierCorporatePermissionListResponse;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

// Verifier 기업 권한 QueryRepository 구현체
@Repository
@RequiredArgsConstructor
public class VerifierCorporatePermissionQueryRepositoryImpl implements VerifierCorporatePermissionQueryRepository {

    private final EntityManager entityManager;

    @Override
    public List<VerifierCorporatePermissionListResponse.Item> findPermissions(
            Long verifierId, // Verifier ID
            Long corporateId, // 법인 ID
            String permissionCode, // 권한 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        TypedQuery<PermissionRow> query = entityManager.createQuery(
                """
                        select new com.kyvc.backend.domain.verifier.repository.VerifierCorporatePermissionQueryRepositoryImpl$PermissionRow(
                            c.corporateId,
                            c.corporateName,
                            v.purpose,
                            v.vpVerificationStatus,
                            v.verifiedAt
                        )
                        from VpVerification v
                        join Corporate c on c.corporateId = v.corporateId
                        where v.verifierId = :verifierId
                          and v.testYn = :testNo
                          and (:corporateId is null or c.corporateId = :corporateId)
                          and (:permissionCode is null
                               or lower(v.purpose) = lower(:permissionCode)
                               or lower(coalesce(v.permissionResultJson, '')) like lower(concat('%', :permissionCode, '%')))
                        order by v.verifiedAt desc nulls last, v.requestedAt desc
                        """,
                PermissionRow.class
        );
        applyPermissionParameters(query, verifierId, corporateId, permissionCode);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream()
                .map(PermissionRow::toItem)
                .toList();
    }

    @Override
    public long countPermissions(
            Long verifierId, // Verifier ID
            Long corporateId, // 법인 ID
            String permissionCode // 권한 코드
    ) {
        TypedQuery<Long> query = entityManager.createQuery(
                """
                        select count(v)
                        from VpVerification v
                        join Corporate c on c.corporateId = v.corporateId
                        where v.verifierId = :verifierId
                          and v.testYn = :testNo
                          and (:corporateId is null or c.corporateId = :corporateId)
                          and (:permissionCode is null
                               or lower(v.purpose) = lower(:permissionCode)
                               or lower(coalesce(v.permissionResultJson, '')) like lower(concat('%', :permissionCode, '%')))
                        """,
                Long.class
        );
        applyPermissionParameters(query, verifierId, corporateId, permissionCode);
        return query.getSingleResult();
    }

    private void applyPermissionParameters(
            TypedQuery<?> query, // JPQL query
            Long verifierId, // Verifier ID
            Long corporateId, // 법인 ID
            String permissionCode // 권한 코드
    ) {
        query.setParameter("verifierId", verifierId);
        query.setParameter("testNo", KyvcEnums.Yn.N);
        query.setParameter("corporateId", corporateId);
        query.setParameter("permissionCode", StringUtils.hasText(permissionCode) ? permissionCode.trim() : null);
    }

    // 기업 권한 확인 조회 행
    public record PermissionRow(
            Long corporateId, // 법인 ID
            String corporateName, // 법인명
            String permissionCode, // 권한 코드
            KyvcEnums.VpVerificationStatus verificationStatus, // 검증 상태
            java.time.LocalDateTime verifiedAt // 검증 일시
    ) {

        private VerifierCorporatePermissionListResponse.Item toItem() {
            String status = verificationStatus == null ? null : verificationStatus.name(); // 검증 상태명
            return new VerifierCorporatePermissionListResponse.Item(
                    corporateId,
                    corporateName,
                    permissionCode,
                    KyvcEnums.VpVerificationStatus.VALID == verificationStatus
                            ? KyvcEnums.VpVerificationStatus.VALID.name()
                            : status,
                    status,
                    verifiedAt
            );
        }
    }
}
