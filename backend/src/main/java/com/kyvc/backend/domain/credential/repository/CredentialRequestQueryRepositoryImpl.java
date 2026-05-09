package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

// Credential 요청 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialRequestQueryRepositoryImpl implements CredentialRequestQueryRepository {

    private final CredentialRequestJpaRepository credentialRequestJpaRepository;

    // Credential ID 목록 기준 요청 이력 조회
    @Override
    public List<CredentialRequest> findByCredentialIds(
            Collection<Long> credentialIds, // Credential ID 목록
            KyvcEnums.CredentialRequestType requestType, // 요청 유형 필터
            KyvcEnums.CredentialRequestStatus requestStatus // 요청 상태 필터
    ) {
        if (credentialIds == null || credentialIds.isEmpty()) {
            return List.of();
        }
        return credentialRequestJpaRepository.findByCredentialIdInOrderByRequestedAtDesc(credentialIds).stream()
                .filter(request -> requestType == null || requestType == request.getRequestType())
                .filter(request -> requestStatus == null || requestStatus == request.getRequestStatus())
                .toList();
    }
}
