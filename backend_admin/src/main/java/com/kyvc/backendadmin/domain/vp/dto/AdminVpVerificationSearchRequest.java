package com.kyvc.backendadmin.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * VP 검증 목록 검색 조건 DTO입니다.
 */
@Schema(description = "VP 검증 목록 검색 조건")
public record AdminVpVerificationSearchRequest(

        /** 페이지 번호 */
        @Schema(description = "페이지 번호, 0부터 시작", example = "0")
        int page,

        /** 페이지 크기 */
        @Schema(description = "페이지 크기", example = "20")
        int size,

        /** VP 검증 상태 코드 */
        @Schema(description = "VP 검증 상태 코드", example = "VALID")
        String status,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "900003")
        Long corporateId,

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** Verifier ID */
        @Schema(description = "Verifier ID", example = "1")
        Long verifierId,

        /** 요청 유형 코드 */
        @Schema(description = "요청 유형 코드", example = "PRESENTATION")
        String requestTypeCode,

        /** Replay 의심 여부 */
        @Schema(description = "Replay 의심 여부", example = "N")
        String replaySuspectedYn,

        /** 테스트 요청 여부 */
        @Schema(description = "테스트 요청 여부", example = "N")
        String testYn,

        /** 요청일 조회 시작일 */
        @Schema(description = "요청일 조회 시작일", example = "2026-05-01")
        LocalDate fromDate,

        /** 요청일 조회 종료일 */
        @Schema(description = "요청일 조회 종료일", example = "2026-05-31")
        LocalDate toDate,

        /** 키워드 */
        @Schema(description = "법인명, 요청자명, 목적 검색 키워드", example = "대출")
        String keyword
) {

    /**
     * 검색 조건의 페이지 값을 보정합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status VP 검증 상태 코드
     * @param corporateId 법인 ID
     * @param credentialId Credential ID
     * @param verifierId Verifier ID
     * @param requestTypeCode 요청 유형 코드
     * @param replaySuspectedYn Replay 의심 여부
     * @param testYn 테스트 요청 여부
     * @param fromDate 요청일 조회 시작일
     * @param toDate 요청일 조회 종료일
     * @param keyword 키워드
     * @return 보정된 검색 조건
     */
    public static AdminVpVerificationSearchRequest of(
            Integer page,
            Integer size,
            String status,
            Long corporateId,
            Long credentialId,
            Long verifierId,
            String requestTypeCode,
            String replaySuspectedYn,
            String testYn,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword
    ) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return new AdminVpVerificationSearchRequest(
                normalizedPage,
                normalizedSize,
                status,
                corporateId,
                credentialId,
                verifierId,
                requestTypeCode,
                replaySuspectedYn,
                testYn,
                fromDate,
                toDate,
                keyword
        );
    }
}
