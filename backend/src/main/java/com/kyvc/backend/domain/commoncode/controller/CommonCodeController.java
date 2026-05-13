package com.kyvc.backend.domain.commoncode.controller;

import com.kyvc.backend.domain.commoncode.application.CommonCodeService;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeBatchResponse;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeGroupListResponse;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공통코드 API Controller
 */
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
@Tag(name = "공통 / 알림 / 코드", description = "공통코드, 사용자 알림, 내부 알림 및 감사로그 API")
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    /**
     * 공통코드 배치 조회
     *
     * @param codeGroups 공통코드 그룹 코드 목록
     * @param enabledOnly 사용 여부 필터
     * @param includeMetadata 메타데이터 포함 여부
     * @return 공통코드 배치 조회 응답
     */
    @Operation(
            summary = "공통코드 배치 조회",
            description = "여러 공통코드 그룹의 코드 목록 일괄 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "공통코드 배치 조회 응답 반환",
            content = @Content(schema = @Schema(implementation = CommonCodeBatchResponse.class))
    )
    @GetMapping("/codes")
    public ResponseEntity<CommonResponse<CommonCodeBatchResponse>> getCodes(
            @Parameter(description = "공통코드 그룹 코드 목록", example = "DOCUMENT_TYPE,CORPORATE_TYPE")
            @RequestParam String codeGroups, // 공통코드 그룹 코드 목록
            @Parameter(description = "사용 여부 필터", example = "true")
            @RequestParam(defaultValue = "true") boolean enabledOnly, // 사용 여부 필터
            @Parameter(description = "메타데이터 포함 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean includeMetadata // 메타데이터 포함 여부
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                commonCodeService.getCodes(codeGroups, enabledOnly, includeMetadata)
        ));
    }

    /**
     * 공통코드 그룹 목록 조회
     *
     * @param enabledOnly 사용 여부 필터
     * @return 공통코드 그룹 목록 응답
     */
    @Operation(
            summary = "공통코드 그룹 목록 조회",
            description = "공통코드 그룹 목록 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "공통코드 그룹 목록 응답 반환",
            content = @Content(schema = @Schema(implementation = CommonCodeGroupListResponse.class))
    )
    @GetMapping("/code-groups")
    public ResponseEntity<CommonResponse<CommonCodeGroupListResponse>> getCodeGroups(
            @Parameter(description = "사용 여부 필터", example = "true")
            @RequestParam(defaultValue = "true") boolean enabledOnly // 사용 여부 필터
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                commonCodeService.getCodeGroups(enabledOnly)
        ));
    }

    /**
     * 공통코드 그룹별 코드 목록 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @param enabledOnly 사용 여부 필터
     * @param includeMetadata 메타데이터 포함 여부
     * @return 공통코드 그룹별 코드 목록 응답
     */
    @Operation(
            summary = "공통코드 그룹별 코드 목록 조회",
            description = "특정 공통코드 그룹의 코드 목록 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "공통코드 그룹별 코드 목록 응답 반환",
            content = @Content(schema = @Schema(implementation = CommonCodeResponse.class))
    )
    @GetMapping("/codes/{codeGroup}")
    public ResponseEntity<CommonResponse<CommonCodeResponse>> getCodeGroupCodes(
            @Parameter(description = "공통코드 그룹 코드", example = "DOCUMENT_TYPE")
            @PathVariable String codeGroup, // 공통코드 그룹 코드
            @Parameter(description = "사용 여부 필터", example = "true")
            @RequestParam(defaultValue = "true") boolean enabledOnly, // 사용 여부 필터
            @Parameter(description = "메타데이터 포함 여부", example = "false")
            @RequestParam(defaultValue = "false") boolean includeMetadata // 메타데이터 포함 여부
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                commonCodeService.getCodeGroupCodes(codeGroup, enabledOnly, includeMetadata)
        ));
    }
}
