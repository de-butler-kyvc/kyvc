package com.kyvc.backend.domain.did.application;

import com.kyvc.backend.domain.did.domain.DidInstitution;
import com.kyvc.backend.domain.did.dto.DidInstitutionResponse;
import com.kyvc.backend.domain.did.repository.DidInstitutionRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// DID 기관 매핑 조회 서비스
@Service
@RequiredArgsConstructor
public class DidInstitutionService {

    private final DidInstitutionRepository didInstitutionRepository;

    // DID 기관명 조회
    @Transactional(readOnly = true)
    public DidInstitutionResponse getInstitution(
            String did // DID
    ) {
        String normalizedDid = normalizeDid(did);
        DidInstitution didInstitution = didInstitutionRepository.findActiveByDid(normalizedDid)
                .orElseThrow(() -> new ApiException(ErrorCode.DID_INSTITUTION_NOT_FOUND));
        return new DidInstitutionResponse(
                didInstitution.getDid(),
                didInstitution.getInstitutionName(),
                didInstitution.getStatusCode().name()
        );
    }

    private String normalizeDid(
            String did // DID
    ) {
        if (!StringUtils.hasText(did)) {
            throw new ApiException(ErrorCode.INVALID_DID);
        }
        String normalizedDid = did.trim(); // 정규화 DID
        if (!normalizedDid.startsWith("did:")) {
            throw new ApiException(ErrorCode.INVALID_DID);
        }
        return normalizedDid;
    }
}
