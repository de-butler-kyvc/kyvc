package com.kyvc.backendadmin.global.commoncode.controller;

import com.kyvc.backendadmin.global.commoncode.application.CommonCodeAdminService;
import com.kyvc.backendadmin.global.commoncode.application.CommonCodeQueryService;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeCreateRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupListResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupSearchRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeListResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeSearchRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeUpdateRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공통코드 그룹/코드 관리 API를 담당합니다.
 */
@Tag(name = "Common Code Admin", description = "백엔드 관리자 공통코드 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class CommonCodeAdminController {

    private final CommonCodeQueryService commonCodeQueryService;
    private final CommonCodeAdminService commonCodeAdminService;

    /**
     * 공통코드 그룹 목록을 조회합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param codeGroup 공통코드 그룹
     * @param keyword 검색어
     * @param enabledYn 사용 여부
     * @return 공통코드 그룹 목록 응답
     */
    @Operation(summary = "공통코드 그룹 목록 조회", description = "공통코드 그룹은 조회만 가능하며 등록, 수정, 삭제 API는 제공하지 않습니다.")
    @GetMapping("/common-code-groups")
    public CommonResponse<CommonCodeGroupListResponse> searchGroups(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "공통코드 그룹", example = "DOCUMENT_TYPE")
            @RequestParam(required = false) String codeGroup,
            @Parameter(description = "공통코드 그룹 또는 그룹명 검색어", example = "DOCUMENT")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "사용 여부", example = "Y")
            @RequestParam(required = false) String enabledYn
    ) {
        // 코드 그룹은 조회만 가능하다는 정책에 따라 목록 조회만 처리한다.
        return CommonResponseFactory.success(commonCodeQueryService.searchGroups(
                CommonCodeGroupSearchRequest.of(page, size, codeGroup, keyword, enabledYn)
        ));
    }

    /**
     * 공통코드 그룹 상세를 조회합니다.
     *
     * @param codeGroupId 공통코드 그룹 ID
     * @return 공통코드 그룹 상세 응답
     */
    @Operation(summary = "공통코드 그룹 상세 조회", description = "공통코드 그룹 ID 기준으로 공통코드 그룹 상세를 조회합니다.")
    @ApiResponse(responseCode = "404", description = "공통코드 그룹이 없는 경우")
    @GetMapping("/common-code-groups/{codeGroupId}")
    public CommonResponse<CommonCodeGroupResponse> getGroup(
            @Parameter(description = "공통코드 그룹 ID", required = true)
            @PathVariable Long codeGroupId
    ) {
        // 코드 그룹은 조회만 가능하다는 정책에 따라 상세 조회만 처리한다.
        return CommonResponseFactory.success(commonCodeQueryService.getGroup(codeGroupId));
    }

    /**
     * 공통코드 목록을 조회합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param codeGroup 공통코드 그룹
     * @param keyword 검색어
     * @param enabledYn 사용 여부
     * @return 공통코드 목록 응답
     */
    @Operation(summary = "공통코드 목록 조회", description = "공통코드 그룹, 검색어, 사용 여부 조건으로 공통코드 목록을 조회합니다.")
    @GetMapping("/common-codes")
    public CommonResponse<CommonCodeListResponse> searchCodes(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "공통코드 그룹", example = "DOCUMENT_TYPE")
            @RequestParam(required = false) String codeGroup,
            @Parameter(description = "코드 또는 코드명 검색어", example = "BUSINESS")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "사용 여부", example = "Y")
            @RequestParam(required = false) String enabledYn
    ) {
        return CommonResponseFactory.success(commonCodeQueryService.searchCodes(
                CommonCodeSearchRequest.of(page, size, codeGroup, keyword, enabledYn)
        ));
    }

    /**
     * 공통코드 상세를 조회합니다.
     *
     * @param codeId 공통코드 ID
     * @return 공통코드 상세 응답
     */
    @Operation(summary = "공통코드 상세 조회", description = "공통코드 ID 기준으로 공통코드 상세를 조회합니다.")
    @ApiResponse(responseCode = "404", description = "공통코드가 없는 경우")
    @GetMapping("/common-codes/{codeId}")
    public CommonResponse<CommonCodeResponse> getCode(
            @Parameter(description = "공통코드 ID", required = true)
            @PathVariable Long codeId
    ) {
        return CommonResponseFactory.success(commonCodeQueryService.getCode(codeId));
    }

    /**
     * 공통코드를 등록합니다.
     *
     * @param request 공통코드 등록 요청
     * @return 등록된 공통코드 응답
     */
    @Operation(summary = "공통코드 등록", description = "공통코드 값을 등록합니다. 동일 그룹 내 code 중복은 허용하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않은 경우"),
            @ApiResponse(responseCode = "404", description = "공통코드 그룹이 없는 경우"),
            @ApiResponse(responseCode = "409", description = "동일 그룹 내 code가 중복된 경우")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/common-codes")
    public CommonResponse<CommonCodeResponse> createCode(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "공통코드 등록 요청", required = true)
            @Valid @org.springframework.web.bind.annotation.RequestBody CommonCodeCreateRequest request
    ) {
        return CommonResponseFactory.success(commonCodeAdminService.create(request));
    }

    /**
     * 공통코드를 수정합니다.
     *
     * @param codeId 공통코드 ID
     * @param request 공통코드 수정 요청
     * @return 수정된 공통코드 응답
     */
    @Operation(summary = "공통코드 수정", description = "공통코드 값을 수정합니다. systemYn=Y 보호 정책으로 수정이 불가할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않은 경우"),
            @ApiResponse(responseCode = "403", description = "systemYn=Y 보호 정책으로 수정할 수 없는 경우"),
            @ApiResponse(responseCode = "404", description = "공통코드가 없는 경우")
    })
    @PatchMapping("/common-codes/{codeId}")
    public CommonResponse<CommonCodeResponse> updateCode(
            @Parameter(description = "공통코드 ID", required = true)
            @PathVariable Long codeId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "공통코드 수정 요청", required = true)
            @Valid @org.springframework.web.bind.annotation.RequestBody CommonCodeUpdateRequest request
    ) {
        return CommonResponseFactory.success(commonCodeAdminService.update(codeId, request));
    }

    /**
     * 공통코드를 활성화합니다.
     *
     * @param codeId 공통코드 ID
     * @return 활성화된 공통코드 응답
     */
    @Operation(summary = "공통코드 활성화", description = "공통코드를 활성화합니다. systemYn=Y 보호 정책으로 변경이 불가할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "systemYn=Y 보호 정책으로 활성화할 수 없는 경우"),
            @ApiResponse(responseCode = "404", description = "공통코드가 없는 경우")
    })
    @PatchMapping("/common-codes/{codeId}/enable")
    public CommonResponse<CommonCodeResponse> enableCode(
            @Parameter(description = "공통코드 ID", required = true)
            @PathVariable Long codeId
    ) {
        return CommonResponseFactory.success(commonCodeAdminService.enable(codeId));
    }

    /**
     * 공통코드를 비활성화합니다.
     *
     * @param codeId 공통코드 ID
     * @return 비활성화된 공통코드 응답
     */
    @Operation(summary = "공통코드 비활성화", description = "공통코드를 비활성화합니다. systemYn=Y 보호 정책으로 변경이 불가할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "systemYn=Y 보호 정책으로 비활성화할 수 없는 경우"),
            @ApiResponse(responseCode = "404", description = "공통코드가 없는 경우")
    })
    @PatchMapping("/common-codes/{codeId}/disable")
    public CommonResponse<CommonCodeResponse> disableCode(
            @Parameter(description = "공통코드 ID", required = true)
            @PathVariable Long codeId
    ) {
        return CommonResponseFactory.success(commonCodeAdminService.disable(codeId));
    }

    /**
     * 공통코드를 삭제합니다.
     *
     * @param codeId 공통코드 ID
     * @return 삭제 성공 응답
     */
    @Operation(summary = "공통코드 삭제", description = "공통코드를 삭제합니다. systemYn=Y 보호 정책으로 삭제가 불가할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "403", description = "systemYn=Y 보호 정책으로 삭제할 수 없는 경우"),
            @ApiResponse(responseCode = "404", description = "공통코드가 없는 경우")
    })
    @DeleteMapping("/common-codes/{codeId}")
    public CommonResponse<Void> deleteCode(
            @Parameter(description = "공통코드 ID", required = true)
            @PathVariable Long codeId
    ) {
        commonCodeAdminService.delete(codeId);
        return CommonResponseFactory.successWithoutData();
    }
}
