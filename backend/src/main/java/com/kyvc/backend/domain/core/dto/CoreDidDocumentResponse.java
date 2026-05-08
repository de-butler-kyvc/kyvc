package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Core DID Document 조회 응답
 *
 * @param account XRPL Account
 * @param didDocument DID Document 객체
 */
@Schema(description = "Core DID Document 조회 응답")
public record CoreDidDocumentResponse(
        @Schema(description = "XRPL Account", example = "rIssuer")
        String account, // XRPL Account
        @Schema(description = "DID Document 객체")
        Map<String, Object> didDocument // DID Document 객체
) {
}
