package com.kyvc.backend.domain.user.controller;

import com.kyvc.backend.domain.user.application.UserProfileService;
import com.kyvc.backend.domain.user.dto.UserMeResponse;
import com.kyvc.backend.domain.user.dto.UserMeUpdateRequest;
import com.kyvc.backend.domain.user.dto.UserMfaUpdateRequest;
import com.kyvc.backend.domain.user.dto.UserPasswordChangeRequest;
import com.kyvc.backend.domain.user.dto.UserProfileUpdateResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 마이페이지 API Controller
 */
@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
@Tag(name = "사용자 계정", description = "사용자 마이페이지 정보, 비밀번호, MFA 설정 API")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 내 정보 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 내 정보 응답
     */
    @Operation(
            summary = "내 정보 조회",
            description = "로그인 사용자의 마이페이지 기본 정보를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 기본 정보 반환",
            content = @Content(schema = @Schema(implementation = UserMeResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<UserMeResponse>> getMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userProfileService.getMe(getAuthenticatedUserId(userDetails))
        ));
    }

    /**
     * 내 정보 수정
     *
     * @param userDetails 인증 사용자 정보
     * @param request 내 정보 수정 요청
     * @return 내 정보 응답
     */
    @Operation(
            summary = "내 정보 수정",
            description = "로그인 사용자의 사용자명, 연락처, 알림 수신 여부를 수정합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "수정된 사용자 기본 정보 반환",
            content = @Content(schema = @Schema(implementation = UserMeResponse.class))
    )
    @PatchMapping
    public ResponseEntity<CommonResponse<UserMeResponse>> updateMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "내 정보 수정 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserMeUpdateRequest.class))
            )
            @Valid @RequestBody UserMeUpdateRequest request // 내 정보 수정 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userProfileService.updateMe(getAuthenticatedUserId(userDetails), request)
        ));
    }

    /**
     * 비밀번호 변경
     *
     * @param userDetails 인증 사용자 정보
     * @param request 비밀번호 변경 요청
     * @return 변경 완료 응답
     */
    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호 확인 후 새 비밀번호를 BCrypt 해시로 저장합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "비밀번호 변경 완료 여부 반환",
            content = @Content(schema = @Schema(implementation = UserProfileUpdateResponse.class))
    )
    @PatchMapping("/password")
    public ResponseEntity<CommonResponse<UserProfileUpdateResponse>> changePassword(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "비밀번호 변경 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserPasswordChangeRequest.class))
            )
            @Valid @RequestBody UserPasswordChangeRequest request // 비밀번호 변경 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userProfileService.changePassword(getAuthenticatedUserId(userDetails), request)
        ));
    }

    /**
     * MFA 설정 변경
     *
     * @param userDetails 인증 사용자 정보
     * @param request MFA 설정 변경 요청
     * @return 변경 완료 응답
     */
    @Operation(
            summary = "MFA 설정 변경",
            description = "MFA 사용 여부를 변경합니다. 활성화 시 검증된 MFA 세션 토큰이 필요합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "MFA 설정 변경 완료 여부 반환",
            content = @Content(schema = @Schema(implementation = UserProfileUpdateResponse.class))
    )
    @PatchMapping("/mfa")
    public ResponseEntity<CommonResponse<UserProfileUpdateResponse>> updateMfa(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "MFA 설정 변경 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserMfaUpdateRequest.class))
            )
            @Valid @RequestBody UserMfaUpdateRequest request // MFA 설정 변경 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userProfileService.updateMfa(getAuthenticatedUserId(userDetails), request)
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
