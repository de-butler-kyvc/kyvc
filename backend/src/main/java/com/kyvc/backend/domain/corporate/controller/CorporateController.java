package com.kyvc.backend.domain.corporate.controller;

import com.kyvc.backend.domain.corporate.application.CorporateService;
import com.kyvc.backend.domain.corporate.dto.CorporateBasicInfoRequest;
import com.kyvc.backend.domain.corporate.dto.CorporateCreateRequest;
import com.kyvc.backend.domain.corporate.dto.CorporateResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 법인 기본정보 API Controller
 */
@RestController
@RequestMapping("/api/user/corporates")
@RequiredArgsConstructor
@Tag(name = "법인 사용자", description = "법인 사용자 대시보드, 법인 기본정보, 대표자, 대리인 관리 API")
public class CorporateController {

    private final CorporateService corporateService;

    /**
     * 법인 기본정보 최초 등록
     *
     * @param userDetails 인증 사용자 정보
     * @param request 법인 기본정보 최초 등록 요청
     * @return 등록된 법인정보 응답
     */
    @Operation(
            summary = "법인 기본정보 최초 등록",
            description = "로그인 사용자의 법인 기본정보를 최초 등록합니다. 입력값은 법인명, 사업자등록번호, 법인등록번호, 법인 유형 코드, 설립일, 법인 대표전화, 주소, 웹사이트입니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "등록된 법인 ID, 사용자 ID, 법인 기본정보, 법인 상태, 생성 및 수정 일시 반환",
            content = @Content(schema = @Schema(implementation = CorporateResponse.class))
    )
    @PostMapping
    public ResponseEntity<CommonResponse<CorporateResponse>> createCorporate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "법인 기본정보 최초 등록 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CorporateCreateRequest.class))
            )
            @Valid @RequestBody CorporateCreateRequest request // 법인 기본정보 최초 등록 요청
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(
                        corporateService.createCorporate(getAuthenticatedUserId(userDetails), request)
                ));
    }

    /**
     * 내 법인정보 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 내 법인정보 응답
     */
    @Operation(
            summary = "내 법인정보 조회",
            description = "로그인 사용자가 등록한 법인 기본정보를 조회합니다. 입력값은 없습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "법인 ID, 사용자 ID, 법인 기본정보, 법인 상태, 생성 및 수정 일시 반환",
            content = @Content(schema = @Schema(implementation = CorporateResponse.class))
    )
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<CorporateResponse>> getMyCorporate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(corporateService.getMyCorporate(getAuthenticatedUserId(userDetails)))
        );
    }

    /**
     * 법인 기본정보 수정
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param request 법인 기본정보 수정 요청
     * @return 수정된 법인정보 응답
     */
    @Operation(
            summary = "법인 기본정보 수정",
            description = "로그인 사용자가 소유한 법인의 기본정보를 수정합니다. 입력값은 법인 ID, 법인명, 사업자등록번호, 법인등록번호, 법인 유형 코드, 설립일, 법인 대표전화, 주소, 웹사이트입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "수정된 법인 ID, 사용자 ID, 법인 기본정보, 법인 상태, 생성 및 수정 일시 반환",
            content = @Content(schema = @Schema(implementation = CorporateResponse.class))
    )
    @PutMapping("/{corporateId}/basic-info")
    public ResponseEntity<CommonResponse<CorporateResponse>> updateBasicInfo(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "법인 기본정보 수정 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CorporateBasicInfoRequest.class))
            )
            @Valid @RequestBody CorporateBasicInfoRequest request // 법인 기본정보 수정 요청
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(
                        corporateService.updateBasicInfo(getAuthenticatedUserId(userDetails), corporateId, request)
                )
        );
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
