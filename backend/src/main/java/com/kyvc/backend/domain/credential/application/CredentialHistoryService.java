package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.dto.CredentialStatusHistoryResponse;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Credential 상태 이력 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CredentialHistoryService {

    private final CredentialRepository credentialRepository;
    private final CredentialStatusHistoryQueryRepository credentialStatusHistoryQueryRepository;
    private final CorporateRepository corporateRepository;

    // Credential 상태 이력 조회
    public CredentialStatusHistoryResponse getCredentialHistories(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId // Credential ID
    ) {
        validateCredentialId(credentialId);
        Long corporateId = resolveCorporateId(resolveUserId(userDetails));
        Credential credential = credentialRepository.getById(credentialId);
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
        List<CredentialStatusHistoryResponse.Item> items = credentialStatusHistoryQueryRepository
                .findByCredentialIdAndCorporateId(credentialId, corporateId)
                .stream()
                .map(this::toItem)
                .toList();
        return new CredentialStatusHistoryResponse(items);
    }

    private CredentialStatusHistoryResponse.Item toItem(
            CredentialStatusHistory history // Credential 상태 이력
    ) {
        return new CredentialStatusHistoryResponse.Item(
                history.getHistoryId(),
                history.getCredentialId(),
                history.getBeforeStatusCode(),
                history.getAfterStatusCode(),
                history.getReason(),
                history.getChangedAt()
        );
    }

    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    private Long resolveCorporateId(
            Long userId // 사용자 ID
    ) {
        Corporate corporate = corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        return corporate.getCorporateId();
    }

    private void validateCredentialId(
            Long credentialId // Credential ID
    ) {
        if (credentialId == null || credentialId < 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
