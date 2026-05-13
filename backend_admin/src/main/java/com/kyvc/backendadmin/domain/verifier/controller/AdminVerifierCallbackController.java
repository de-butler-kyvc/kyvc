package com.kyvc.backendadmin.domain.verifier.controller;

import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierCallbackService;
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
 * Verifier Callback 관리 API를 제공합니다.
 */
@Tag(name = "Backend Admin Verifier Callback", description = "Verifier Callback URL 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/verifiers/{verifierId}/callbacks")
public class AdminVerifierCallbackController {

    private final AdminVerifierCallbackService callbackService;

    @Operation(summary = "Verifier Callback 조회", description = "Verifier Callback URL 목록을 조회한다.")
    @GetMapping
    public CommonResponse<List<AdminVerifierDtos.CallbackResponse>> getCallbacks(@PathVariable Long verifierId) {
        return CommonResponseFactory.success(callbackService.getCallbacks(verifierId));
    }

    @Operation(summary = "Verifier Callback 변경", description = "Callback URL 형식을 검증한 뒤 새 Callback 설정을 저장한다.")
    @PatchMapping
    public CommonResponse<List<AdminVerifierDtos.CallbackResponse>> update(
            @PathVariable Long verifierId,
            @Valid @RequestBody AdminVerifierDtos.CallbackUpdateRequest request
    ) {
        return CommonResponseFactory.success(callbackService.update(verifierId, request));
    }
}
