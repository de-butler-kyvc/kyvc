package com.kyvc.backendadmin.domain.review.controller;

import com.kyvc.backendadmin.domain.review.application.AiReviewPolicyQueryService;
import com.kyvc.backendadmin.domain.review.application.AiReviewPolicyService;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyCreateRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyEnabledUpdateRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySearchRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySummaryResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyUpdateRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backend Admin AI 심사 업무 정책 관리 API를 담당합니다.
 */
@Tag(name = "Backend Admin AI Review Policy", description = "백엔드 관리자 AI 심사 업무 정책 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/ai-review-policies")
public class AdminAiReviewPolicyController {

    private final AiReviewPolicyQueryService aiReviewPolicyQueryService;
    private final AiReviewPolicyService aiReviewPolicyService;

    /**
     * AI 심사 업무 정책 목록을 조회합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param keyword 검색어
     * @param status 정책 상태
     * @param corporateType 법인 유형
     * @param enabledYn 사용 여부
     * @return AI 심사 업무 정책 목록
     */
    @Operation(
            summary = "AI 심사 정책 목록 조회",
            description = "AI 심사 업무 정책 목록을 페이징, 키워드, 상태, 법인 유형, 사용 여부 조건으로 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 심사 정책 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "검색 조건이 유효하지 않음")
    })
    @GetMapping
    public CommonResponse<AiReviewPolicySummaryResponse> search(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "정책명 검색어", example = "기본")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정책 상태(ACTIVE/INACTIVE)", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "법인 유형 코드", example = "CORPORATION")
            @RequestParam(required = false) String corporateType,
            @Parameter(description = "사용 여부(Y/N)", example = "Y")
            @RequestParam(required = false) String enabledYn
    ) {
        return CommonResponseFactory.success(aiReviewPolicyQueryService.search(
                AiReviewPolicySearchRequest.of(page, size, keyword, status, corporateType, enabledYn)
        ));
    }

    /**
     * AI 심사 업무 정책 상세 정보를 조회합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @return AI 심사 업무 정책 상세 정보
     */
    @Operation(
            summary = "AI 심사 정책 상세 조회",
            description = "AI 심사 업무 정책 ID를 기준으로 정책 상세 정보를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 심사 정책 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "AI 심사 정책을 찾을 수 없음")
    })
    @GetMapping("/{aiPolicyId}")
    public CommonResponse<AiReviewPolicyResponse> getDetail(
            @Parameter(description = "AI 심사 정책 ID", required = true)
            @PathVariable Long aiPolicyId
    ) {
        return CommonResponseFactory.success(aiReviewPolicyQueryService.getDetail(aiPolicyId));
    }

    /**
     * AI 심사 업무 정책을 등록합니다.
     *
     * @param request AI 심사 정책 등록 요청
     * @return 등록된 AI 심사 업무 정책 상세 정보
     */
    @Operation(
            summary = "AI 심사 정책 등록",
            description = "AI 심사 결과를 자동 승인, 수동 심사, 보완요청 후보로 분기하기 위한 업무 정책을 등록한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 심사 정책 등록 성공"),
            @ApiResponse(responseCode = "400", description = "정책 값이 유효하지 않음")
    })
    @PostMapping
    public CommonResponse<AiReviewPolicyResponse> create(
            @RequestBody(
                    description = "AI 심사 정책 등록 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AiReviewPolicyCreateRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AiReviewPolicyCreateRequest request
    ) {
        return CommonResponseFactory.success(aiReviewPolicyService.create(request));
    }

    /**
     * AI 심사 업무 정책을 수정합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @param request AI 심사 정책 수정 요청
     * @return 수정된 AI 심사 업무 정책 상세 정보
     */
    @Operation(
            summary = "AI 심사 정책 수정",
            description = "AI 심사 업무 정책의 이름, threshold, 사용 기준, 설명을 수정한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 심사 정책 수정 성공"),
            @ApiResponse(responseCode = "400", description = "정책 값이 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "AI 심사 정책을 찾을 수 없음")
    })
    @PatchMapping("/{aiPolicyId}")
    public CommonResponse<AiReviewPolicyResponse> update(
            @Parameter(description = "AI 심사 정책 ID", required = true)
            @PathVariable Long aiPolicyId,
            @RequestBody(
                    description = "AI 심사 정책 수정 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AiReviewPolicyUpdateRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AiReviewPolicyUpdateRequest request
    ) {
        return CommonResponseFactory.success(aiReviewPolicyService.update(aiPolicyId, request));
    }

    /**
     * AI 심사 업무 정책 사용 여부를 변경합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @param request 사용 여부 변경 요청
     * @return 변경된 AI 심사 업무 정책 상세 정보
     */
    @Operation(
            summary = "AI 심사 정책 사용 여부 변경",
            description = "AI 심사 업무 정책의 enabledYn 값을 변경한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 심사 정책 사용 여부 변경 성공"),
            @ApiResponse(responseCode = "400", description = "사용 여부 값이 유효하지 않음"),
            @ApiResponse(responseCode = "404", description = "AI 심사 정책을 찾을 수 없음")
    })
    @PatchMapping("/{aiPolicyId}/enabled")
    public CommonResponse<AiReviewPolicyResponse> changeEnabled(
            @Parameter(description = "AI 심사 정책 ID", required = true)
            @PathVariable Long aiPolicyId,
            @RequestBody(
                    description = "AI 심사 정책 사용 여부 변경 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AiReviewPolicyEnabledUpdateRequest.class))
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody AiReviewPolicyEnabledUpdateRequest request
    ) {
        return CommonResponseFactory.success(aiReviewPolicyService.changeEnabled(aiPolicyId, request));
    }
}
