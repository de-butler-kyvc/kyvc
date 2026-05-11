package com.kyvc.backend.domain.verifier.controller;

import com.kyvc.backend.domain.verifier.application.VerifierAppService;
import com.kyvc.backend.domain.verifier.application.VerifierCorporatePermissionService;
import com.kyvc.backend.domain.verifier.application.VerifierIntegrationLogService;
import com.kyvc.backend.domain.verifier.application.VerifierPolicySyncHistoryService;
import com.kyvc.backend.domain.verifier.application.VerifierRuntimeService;
import com.kyvc.backend.domain.verifier.application.VerifierUsageStatsService;
import com.kyvc.backend.domain.verifier.dto.VerifierAppMeResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierCorporatePermissionListResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierIntegrationLogListResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierPolicySyncHistoryListResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationDetailResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierUsageStatsResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.VerifierPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 외부 Verifier Runtime API Controller
 */
@RestController
@RequestMapping("/api/verifier")
@RequiredArgsConstructor
@Tag(name = "Verifier Runtime", description = "외부 Verifier API Key 기반 Runtime API")
public class VerifierController {

    private final VerifierRuntimeService verifierRuntimeService;
    private final VerifierCorporatePermissionService verifierCorporatePermissionService;
    private final VerifierAppService verifierAppService;
    private final VerifierIntegrationLogService verifierIntegrationLogService;
    private final VerifierUsageStatsService verifierUsageStatsService;
    private final VerifierPolicySyncHistoryService verifierPolicySyncHistoryService;

    /**
     * 외부 Verifier 재인증 VP 요청 생성
     *
     * @param principal 인증된 Verifier 주체
     * @param request 재인증 요청
     * @return 재인증 요청 생성 응답
     */
    @Operation(summary = "외부 Verifier 재인증 VP 요청 생성")
    @ApiResponse(responseCode = "201", description = "재인증 요청 생성 응답",
            content = @Content(schema = @Schema(implementation = VerifierReAuthRequestCreateResponse.class)))
    @PostMapping("/re-auth-requests")
    public ResponseEntity<CommonResponse<VerifierReAuthRequestCreateResponse>> createReAuthRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @Valid @RequestBody VerifierReAuthRequestCreateRequest request // 재인증 요청
    ) {
        return ResponseEntity.status(201).body(CommonResponseFactory.success(
                verifierRuntimeService.createReAuthRequest(principal, request)
        ));
    }

    /**
     * 외부 Verifier 기업 권한 확인 결과 목록 조회
     *
     * @param principal 인증된 Verifier 주체
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param corporateId 법인 ID
     * @param permissionCode 권한 코드
     * @return 기업 권한 확인 결과 목록 응답
     */
    @Operation(summary = "외부 Verifier 기업 권한 확인 결과 목록 조회")
    @GetMapping("/corporate-permissions")
    public CommonResponse<VerifierCorporatePermissionListResponse> getCorporatePermissions(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @RequestParam(required = false) Integer page, // 페이지 번호
            @RequestParam(required = false) Integer size, // 페이지 크기
            @RequestParam(required = false) Long corporateId, // 법인 ID
            @RequestParam(required = false) String permissionCode // 권한 코드
    ) {
        return CommonResponseFactory.success(verifierCorporatePermissionService.getPermissions(
                principal,
                page,
                size,
                corporateId,
                permissionCode
        ));
    }

    /**
     * 외부 Verifier 테스트 VP 검증 실행
     *
     * @param principal 인증된 Verifier 주체
     * @param request 테스트 VP 검증 요청
     * @return 테스트 VP 검증 응답
     */
    @Operation(summary = "외부 Verifier 테스트 VP 검증 실행")
    @ApiResponse(responseCode = "201", description = "테스트 VP 검증 응답",
            content = @Content(schema = @Schema(implementation = VerifierTestVpVerificationResponse.class)))
    @PostMapping("/test-vp-verifications")
    public ResponseEntity<CommonResponse<VerifierTestVpVerificationResponse>> testVpVerification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @Valid @RequestBody VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        return ResponseEntity.status(201).body(CommonResponseFactory.success(
                verifierRuntimeService.testVpVerification(principal, request)
        ));
    }

    /**
     * 외부 Verifier 테스트 VP 검증 결과 조회
     *
     * @param principal 인증된 Verifier 주체
     * @param testId 테스트 검증 ID
     * @return 테스트 VP 검증 상세 응답
     */
    @Operation(summary = "외부 Verifier 테스트 VP 검증 결과 조회")
    @GetMapping("/test-vp-verifications/{testId}")
    public CommonResponse<VerifierTestVpVerificationDetailResponse> getTestVpVerification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @PathVariable Long testId // 테스트 검증 ID
    ) {
        return CommonResponseFactory.success(verifierRuntimeService.getTestVpVerification(principal, testId));
    }

    /**
     * 외부 Verifier 앱 정보 조회
     *
     * @param principal 인증된 Verifier 주체
     * @return Verifier 앱 정보 응답
     */
    @Operation(summary = "외부 Verifier 앱 정보 조회")
    @GetMapping("/app/me")
    public CommonResponse<VerifierAppMeResponse> getMe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal // 인증된 Verifier 주체
    ) {
        return CommonResponseFactory.success(verifierAppService.getMe(principal));
    }

    /**
     * 외부 Verifier 연동 로그 조회
     *
     * @param principal 인증된 Verifier 주체
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param actionTypeCode 작업 유형 코드
     * @param from 시작 일자
     * @param to 종료 일자
     * @return 연동 로그 목록 응답
     */
    @Operation(summary = "외부 Verifier 연동 로그 조회")
    @GetMapping("/integration-logs")
    public CommonResponse<VerifierIntegrationLogListResponse> getIntegrationLogs(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @RequestParam(required = false) Integer page, // 페이지 번호
            @RequestParam(required = false) Integer size, // 페이지 크기
            @RequestParam(required = false) String actionTypeCode, // 작업 유형 코드
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, // 시작 일자
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to // 종료 일자
    ) {
        return CommonResponseFactory.success(verifierIntegrationLogService.getLogs(
                principal,
                page,
                size,
                actionTypeCode,
                from,
                to
        ));
    }

    /**
     * 외부 Verifier 사용량 통계 조회
     *
     * @param principal 인증된 Verifier 주체
     * @param from 시작 일자
     * @param to 종료 일자
     * @param unit 집계 단위
     * @return 사용량 통계 응답
     */
    @Operation(summary = "외부 Verifier 사용량 통계 조회")
    @GetMapping("/usage-stats")
    public CommonResponse<VerifierUsageStatsResponse> getUsageStats(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, // 시작 일자
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, // 종료 일자
            @RequestParam(required = false) String unit // 집계 단위
    ) {
        return CommonResponseFactory.success(verifierUsageStatsService.getStats(principal, from, to, unit));
    }

    /**
     * 외부 Verifier 사용량 통계 CSV 다운로드
     *
     * @param principal 인증된 Verifier 주체
     * @param from 시작 일자
     * @param to 종료 일자
     * @param format 파일 형식
     * @return CSV 파일 응답
     */
    @Operation(summary = "외부 Verifier 사용량 통계 CSV 다운로드")
    @GetMapping("/usage-stats/export")
    public ResponseEntity<byte[]> exportUsageStats(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, // 시작 일자
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, // 종료 일자
            @RequestParam String format // 파일 형식
    ) {
        byte[] csv = verifierUsageStatsService.exportCsv(principal, from, to, format);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("verifier-usage-stats.csv", StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(csv);
    }

    /**
     * 외부 Verifier 정책 동기화 이력 조회
     *
     * @param principal 인증된 Verifier 주체
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param from 시작 일자
     * @param to 종료 일자
     * @return 정책 동기화 이력 목록 응답
     */
    @Operation(summary = "외부 Verifier 정책 동기화 이력 조회")
    @GetMapping("/policy-sync-histories")
    public CommonResponse<VerifierPolicySyncHistoryListResponse> getPolicySyncHistories(
            @Parameter(hidden = true)
            @AuthenticationPrincipal VerifierPrincipal principal, // 인증된 Verifier 주체
            @RequestParam(required = false) Integer page, // 페이지 번호
            @RequestParam(required = false) Integer size, // 페이지 크기
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, // 시작 일자
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to // 종료 일자
    ) {
        return CommonResponseFactory.success(verifierPolicySyncHistoryService.getHistories(
                principal,
                page,
                size,
                from,
                to
        ));
    }
}
