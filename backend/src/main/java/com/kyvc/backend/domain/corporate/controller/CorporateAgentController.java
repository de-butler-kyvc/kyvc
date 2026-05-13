package com.kyvc.backend.domain.corporate.controller;

import com.kyvc.backend.domain.corporate.application.CorporateAgentService;
import com.kyvc.backend.domain.corporate.dto.AgentAuthorityResponse;
import com.kyvc.backend.domain.corporate.dto.AgentAuthorityUpdateRequest;
import com.kyvc.backend.domain.corporate.dto.AgentRequest;
import com.kyvc.backend.domain.corporate.dto.AgentResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 대리인 정보 API Controller
 */
@RestController
@RequestMapping("/api/user/corporates/{corporateId}/agents")
@RequiredArgsConstructor
@Tag(name = "법인 사용자", description = "법인 사용자 대시보드, 법인 기본정보, 대표자, 대리인 관리 API")
public class CorporateAgentController {

    private final CorporateAgentService agentService;

    /**
     * 대리인 정보 등록 또는 저장
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param request 대리인 정보 저장 요청
     * @return 저장된 대리인 정보 응답
     */
    @Operation(
            summary = "대리인 정보 등록 또는 저장",
            description = "로그인 사용자가 소유한 법인의 대리인 정보를 저장합니다. 입력값은 법인 ID, 대리인명, 관계/직책, 연락처, 이메일, 위임장 파일입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "대리인 ID, 법인 ID, 대리인명, 관계/직책, 연락처, 이메일 반환",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<AgentResponse>> saveAgent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "대리인 정보 저장 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AgentRequest.class))
            )
            @Valid @ModelAttribute AgentRequest request // 대리인 정보 저장 요청
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(agentService.saveAgent(
                        getAuthenticatedUserId(userDetails),
                        corporateId,
                        request
                ))
        );
    }

    /**
     * 대리인 정보 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @return 대리인 정보 목록 응답
     */
    @Operation(
            summary = "대리인 정보 조회",
            description = "로그인 사용자가 소유한 법인의 대리인 정보를 조회합니다. 현재 DB 구조상 대리인은 법인당 1명이며 목록으로 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "대리인 정보 목록 반환",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AgentResponse.class)))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<List<AgentResponse>>> getAgents(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId // 법인 ID
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(agentService.getAgents(getAuthenticatedUserId(userDetails), corporateId))
        );
    }

    /**
     * 대리인 정보 수정
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param agentId 대리인 ID
     * @param request 대리인 정보 수정 요청
     * @return 수정된 대리인 정보 응답
     */
    @Operation(
            summary = "대리인 정보 수정",
            description = "로그인 사용자가 소유한 법인의 대리인 정보를 수정합니다. 입력값은 대리인명, 관계/직책, 연락처, 이메일, 위임장 파일입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "수정된 대리인 ID, 법인 ID, 대리인명, 관계/직책, 연락처, 이메일 반환",
            content = @Content(schema = @Schema(implementation = AgentResponse.class))
    )
    @PutMapping(value = "/{agentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<AgentResponse>> updateAgent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @Parameter(description = "대리인 ID", example = "1")
            @PathVariable Long agentId, // 대리인 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "대리인 정보 수정 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AgentRequest.class))
            )
            @Valid @ModelAttribute AgentRequest request // 대리인 정보 수정 요청
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(agentService.updateAgent(
                        getAuthenticatedUserId(userDetails),
                        corporateId,
                        agentId,
                        request
                ))
        );
    }

    /**
     * 대리인 권한 수정
     *
     * @param userDetails 인증 사용자 정보
     * @param corporateId 법인 ID
     * @param agentId 대리인 ID
     * @param request 대리인 권한 수정 요청
     * @return 대리인 권한 응답
     */
    @Operation(
            summary = "대리인 권한 수정",
            description = "로그인 사용자가 소유한 법인의 대리인 권한 범위와 권한 상태를 수정합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "수정된 대리인 권한 반환",
            content = @Content(schema = @Schema(implementation = AgentAuthorityResponse.class))
    )
    @PatchMapping("/{agentId}/authority")
    public ResponseEntity<CommonResponse<AgentAuthorityResponse>> updateAuthority(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "법인 ID", example = "1")
            @PathVariable Long corporateId, // 법인 ID
            @Parameter(description = "대리인 ID", example = "1")
            @PathVariable Long agentId, // 대리인 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "대리인 권한 수정 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AgentAuthorityUpdateRequest.class))
            )
            @Valid @RequestBody AgentAuthorityUpdateRequest request // 대리인 권한 수정 요청
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(agentService.updateAuthority(
                        getAuthenticatedUserId(userDetails),
                        corporateId,
                        agentId,
                        request
                ))
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
