package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Credential 요청 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialRequestRepositoryImpl implements CredentialRequestRepository {

    private static final List<KyvcEnums.CredentialRequestStatus> IN_PROGRESS_STATUSES = List.of(
            KyvcEnums.CredentialRequestStatus.REQUESTED,
            KyvcEnums.CredentialRequestStatus.PROCESSING
    ); // 진행 중 요청 상태 목록

    private final CredentialRequestJpaRepository credentialRequestJpaRepository;

    // Credential 요청 저장
    @Override
    public CredentialRequest save(
            CredentialRequest credentialRequest // 저장 대상 Credential 요청
    ) {
        return credentialRequestJpaRepository.save(credentialRequest);
    }

    // Credential 요청 ID 기준 조회
    @Override
    public Optional<CredentialRequest> findById(
            Long credentialRequestId // Credential 요청 ID
    ) {
        return credentialRequestJpaRepository.findByCredentialRequestId(credentialRequestId);
    }

    // Credential 요청 ID 기준 필수 조회
    @Override
    public CredentialRequest getById(
            Long credentialRequestId // Credential 요청 ID
    ) {
        return findById(credentialRequestId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_REQUEST_NOT_FOUND));
    }

    // Credential별 진행 중 요청 존재 여부 조회
    @Override
    public boolean existsInProgressByCredentialIdAndType(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestTypeCode // 요청 유형
    ) {
        return existsInProgress(credentialId, requestTypeCode);
    }

    // 진행 중 요청 존재 여부
    @Override
    public boolean existsInProgress(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestType // 요청 유형
    ) {
        return credentialRequestJpaRepository.existsByCredentialIdAndRequestTypeAndRequestStatusIn(
                credentialId,
                requestType,
                IN_PROGRESS_STATUSES
        );
    }
}
