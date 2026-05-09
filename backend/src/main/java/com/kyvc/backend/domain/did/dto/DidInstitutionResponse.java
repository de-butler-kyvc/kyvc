package com.kyvc.backend.domain.did.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DID 기관명 조회 응답
 *
 * @param did DID
 * @param institutionName 기관명
 * @param status 상태
 */
@Schema(description = "DID 기관명 조회 응답")
public record DidInstitutionResponse(
        @Schema(description = "DID", example = "did:xrpl:1:rIssuer")
        String did, // DID
        @Schema(description = "기관명", example = "KYvC Issuer")
        String institutionName, // 기관명
        @Schema(description = "상태", example = "ACTIVE")
        String status // 상태
) {
}
