package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.credential.domain.Credential;

import java.util.Map;

// Holder 전달용 VC 발급 결과
public record CredentialIssuanceResult(
        Credential credential, // 저장된 Credential 메타데이터
        String format, // VC format
        String compactCredential, // Holder 전달용 compact Credential, DB 저장 금지
        Map<String, Object> credentialObject, // legacy credential object, DB 저장 금지
        Map<String, Object> selectiveDisclosure // 선택공개 정보, DB 저장 금지
) {
}
