package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Core Credential 검증 요청
 *
 * @param format Credential format
 * @param credential Credential 원문
 * @param didDocuments DID Document 맵
 * @param policy Policy 객체
 */
@Schema(description = "Core Credential 검증 요청")
public record CoreCredentialVerificationRequest(
        @Schema(description = "Credential format", example = "vc+jwt")
        String format, // Credential format
        @Schema(description = "Credential 원문")
        Object credential, // Credential 원문
        @Schema(description = "DID Document 맵")
        Map<String, Object> didDocuments, // DID Document 맵
        @Schema(description = "Policy 객체")
        Map<String, Object> policy // Policy 객체
) {
}