package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// Credential 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialQueryRepositoryImpl implements CredentialQueryRepository {

    private final CredentialJpaRepository credentialJpaRepository;

    // 법인 ID 기준 Credential 목록 조회
    @Override
    public List<Credential> findByCorporateId(
            Long corporateId, // 법인 ID
            KyvcEnums.CredentialStatus credentialStatus // Credential 상태 필터
    ) {
        if (credentialStatus == null) {
            return credentialJpaRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId);
        }
        return credentialJpaRepository.findByCorporateIdAndCredentialStatusOrderByCreatedAtDesc(
                corporateId,
                credentialStatus
        );
    }
}
