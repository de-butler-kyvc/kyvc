package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

// Core /issuer/credentials/revoke 응답 DTO
public record RevokeCredentialApiResponse(
        @JsonProperty("revoked")
        boolean revoked, // 폐기 성공 여부
        @JsonProperty("credential_delete_transaction")
        Map<String, Object> credentialDeleteTransaction, // 폐기 트랜잭션 객체
        @JsonProperty("ledger_entry")
        Map<String, Object> ledgerEntry, // Ledger entry 객체
        @JsonProperty("status_mode")
        String statusMode // 상태 저장 모드
) {
}
