package com.kyvc.backend.domain.mobile.controller;

import com.kyvc.backend.domain.mobile.application.MobileDeviceService;
import com.kyvc.backend.domain.mobile.application.MobileSecurityService;
import com.kyvc.backend.domain.mobile.dto.MobileDeviceRegisterRequest;
import com.kyvc.backend.domain.mobile.dto.MobileDeviceRegisterResponse;
import com.kyvc.backend.domain.mobile.dto.MobileSecuritySettingRequest;
import com.kyvc.backend.domain.mobile.dto.MobileSecuritySettingResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모바일 기기 및 보안 설정 API Controller
 */
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Tag(name = "VC / Wallet", description = "모바일 기기, Wallet, Credential Offer 수락, VC 보관 및 조회 API")
public class MobileDeviceController {

    private final MobileDeviceService mobileDeviceService;
    private final MobileSecurityService mobileSecurityService;

    /**
     * 모바일 기기 등록
     *
     * @param userDetails 인증 사용자 정보
     * @param request 모바일 기기 등록 요청
     * @return 모바일 기기 등록 응답
     */
    @Operation(
            summary = "모바일 기기 등록",
            description = "로그인 사용자의 모바일 기기 정보를 등록하거나 갱신합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 기기 등록 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileDeviceRegisterResponse.class))
    )
    @PostMapping("/device/register")
    public ResponseEntity<CommonResponse<MobileDeviceRegisterResponse>> registerDevice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody MobileDeviceRegisterRequest request // 모바일 기기 등록 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mobileDeviceService.registerDevice(getAuthenticatedUserId(userDetails), request)
        ));
    }

    /**
     * 모바일 보안 설정 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param deviceId 모바일 기기 ID
     * @return 모바일 보안 설정 응답
     */
    @Operation(
            summary = "모바일 보안 설정 조회",
            description = "로그인 사용자의 모바일 기기 보안 설정을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 보안 설정 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileSecuritySettingResponse.class))
    )
    @GetMapping("/settings/security")
    public ResponseEntity<CommonResponse<MobileSecuritySettingResponse>> getSecuritySetting(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "모바일 기기 ID", example = "device-001")
            @RequestParam String deviceId // 모바일 기기 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mobileSecurityService.getSecuritySetting(getAuthenticatedUserId(userDetails), deviceId)
        ));
    }

    /**
     * 모바일 보안 설정 저장
     *
     * @param userDetails 인증 사용자 정보
     * @param request 모바일 보안 설정 저장 요청
     * @return 모바일 보안 설정 응답
     */
    @Operation(
            summary = "모바일 보안 설정 저장",
            description = "로그인 사용자의 모바일 기기 보안 설정을 저장합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 보안 설정 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileSecuritySettingResponse.class))
    )
    @PutMapping("/settings/security")
    public ResponseEntity<CommonResponse<MobileSecuritySettingResponse>> updateSecuritySetting(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody MobileSecuritySettingRequest request // 모바일 보안 설정 저장 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mobileSecurityService.updateSecuritySetting(getAuthenticatedUserId(userDetails), request)
        ));
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
