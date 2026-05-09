package com.kyvc.backend.domain.kyc.controller;

import com.kyvc.backend.domain.kyc.application.DevKycApprovalService;
import com.kyvc.backend.domain.kyc.dto.DevKycApproveRequest;
import com.kyvc.backend.domain.kyc.dto.DevKycApproveResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발/E2E 테스트용 KYC 임시 승인 API Controller.
 * 운영용 backend-admin 승인 API 구현 시 제거 또는 비활성화 대상
 */
@RestController
@RequestMapping("/api/internal/dev/kyc")
@RequiredArgsConstructor
@Tag(name = "Core 연동", description = "Core 내부 헬스체크, Issuer 정책 및 개발용 E2E API")
public class DevKycApprovalController {

    private final DevKycApprovalService devKycApprovalService;

    /**
     * 개발/E2E 테스트용 KYC 임시 승인 API.
     *
     * @param kycId KYC 신청 ID
     * @param request 임시 승인 요청
     * @return 임시 승인 및 VC 발급 요청 결과
     */
    @Operation(
            summary = "개발/E2E 테스트용 KYC 임시 승인",
            description = "개발/E2E 테스트용 임시 API, 운영용 backend-admin 승인 API 구현 시 제거 또는 비활성화 대상"
    )
    @PostMapping("/applications/{kycId}/approve")
    public ResponseEntity<CommonResponse<DevKycApproveResponse>> approveForE2eTest(
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @RequestBody(required = false) DevKycApproveRequest request // 임시 승인 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(devKycApprovalService.approveForE2eTest(kycId, request)));
    }
}
