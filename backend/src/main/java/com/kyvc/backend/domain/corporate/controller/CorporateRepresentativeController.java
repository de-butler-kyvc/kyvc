package com.kyvc.backend.domain.corporate.controller;

import com.kyvc.backend.domain.corporate.application.CorporateRepresentativeService;
import com.kyvc.backend.domain.corporate.dto.RepresentativeRequest;
import com.kyvc.backend.domain.corporate.dto.RepresentativeResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대표자 정보 API Controller
 */
@RestController
@RequestMapping("/api/user/corporates/{corporateId}/representatives")
@RequiredArgsConstructor
@Tag(name = "법인 사용자", description = "법인 사용자 대시보드, 법인 기본정보, 대표자, 대리인 관리 API")
public class CorporateRepresentativeController {

    private final CorporateRepresentativeService representativeService;

    /**
     * 대표자 정보 등록 또는 저장
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param request 대표자 정보 저장 요청
     * @return 저장된 대표자 정보 응답
     */
    @Operation(
            summary = "대표자 정보 등록 또는 저장",
            description = "로그인 사용자가 소유한 법인의 대표자 정보를 저장합니다. 입력값은 법인 ID, 대표자명, 생년월일, 연락처, 이메일, 신분증 사본 파일입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "대표자 ID, 법인 ID, 대표자명, 연락처, 이메일 반환",
            content = @Content(schema = @Schema(implementation = RepresentativeResponse.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<RepresentativeResponse>> saveRepresentative(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "대표자 정보 저장 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RepresentativeRequest.class))
            )
            @Valid @ModelAttribute RepresentativeRequest request // 대표자 정보 저장 요청
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(
                        representativeService.saveRepresentative(getAuthenticatedUserId(userDetails), corporateId, request)
                )
        );
    }

    /**
     * 대표자 정보 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @return 대표자 정보 목록 응답
     */
    @Operation(
            summary = "대표자 정보 조회",
            description = "로그인 사용자가 소유한 법인의 대표자 정보를 조회합니다. 현재 DB 구조상 대표자는 법인당 1명이며 목록으로 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "대표자 정보 목록 반환",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RepresentativeResponse.class)))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<List<RepresentativeResponse>>> getRepresentatives(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId // 법인 ID
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(
                        representativeService.getRepresentatives(getAuthenticatedUserId(userDetails), corporateId)
                )
        );
    }

    /**
     * 대표자 정보 수정
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param representativeId 대표자 ID
     * @param request 대표자 정보 수정 요청
     * @return 수정된 대표자 정보 응답
     */
    @Operation(
            summary = "대표자 정보 수정",
            description = "로그인 사용자가 소유한 법인의 대표자 정보를 수정합니다. 신분증 사본 파일이 포함되면 새 파일로 교체합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "수정된 대표자 ID, 법인 ID, 대표자명, 연락처, 이메일 반환",
            content = @Content(schema = @Schema(implementation = RepresentativeResponse.class))
    )
    @PutMapping(value = "/{representativeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<RepresentativeResponse>> updateRepresentative(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @Parameter(description = "대표자 ID", example = "1")
            @PathVariable Long representativeId, // 대표자 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "대표자 정보 수정 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RepresentativeRequest.class))
            )
            @Valid @ModelAttribute RepresentativeRequest request // 대표자 정보 수정 요청
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(
                        representativeService.updateRepresentative(
                                getAuthenticatedUserId(userDetails),
                                corporateId,
                                representativeId,
                                request
                        )
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
