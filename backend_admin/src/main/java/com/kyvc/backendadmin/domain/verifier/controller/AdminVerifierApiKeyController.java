package com.kyvc.backendadmin.domain.verifier.controller;

import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierApiKeyService;
import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Verifier API Key 관리 API를 제공합니다.
 */
@Tag(name = "Backend Admin Verifier API Key", description = "Verifier SDK 연동 API Key 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/verifiers/{verifierId}/keys")
public class AdminVerifierApiKeyController {

    private final AdminVerifierApiKeyService apiKeyService;

    @Operation(summary = "Verifier API Key 목록 조회", description = "API Key 해시나 secret 원문 없이 prefix와 상태만 조회한다.")
    @GetMapping
    public CommonResponse<List<AdminVerifierDtos.ApiKeyResponse>> getKeys(@PathVariable Long verifierId) {
        return CommonResponseFactory.success(apiKeyService.getKeys(verifierId));
    }

    @Operation(summary = "Verifier API Key 발급", description = "신규 API Key를 발급하고 secret 원문은 최초 1회만 응답한다.")
    @PostMapping
    public CommonResponse<AdminVerifierDtos.ApiKeySecretResponse> create(
            @PathVariable Long verifierId,
            @Valid @RequestBody AdminVerifierDtos.ApiKeyCreateRequest request
    ) {
        return CommonResponseFactory.success(apiKeyService.create(verifierId, request));
    }

    @Operation(summary = "Verifier API Key 회전", description = "기존 Key를 ROTATED 처리하고 신규 secret을 최초 1회만 응답한다.")
    @PostMapping("/{keyId}/rotate")
    public CommonResponse<AdminVerifierDtos.ApiKeySecretResponse> rotate(@PathVariable Long verifierId, @PathVariable Long keyId) {
        return CommonResponseFactory.success(apiKeyService.rotate(verifierId, keyId));
    }

    @Operation(summary = "Verifier API Key 폐기", description = "API Key를 REVOKED 상태로 변경한다.")
    @PostMapping("/{keyId}/revoke")
    public CommonResponse<AdminVerifierDtos.ApiKeyResponse> revoke(
            @PathVariable Long verifierId,
            @PathVariable Long keyId,
            @RequestBody(required = false) AdminVerifierDtos.ApiKeyRevokeRequest request
    ) {
        return CommonResponseFactory.success(apiKeyService.revoke(verifierId, keyId, request));
    }
}
