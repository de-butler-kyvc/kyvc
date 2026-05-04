package com.kyvc.backend.domain.kyc.dto;

import com.kyvc.backend.domain.document.dto.KycDocumentResponse;
import com.kyvc.backend.domain.document.dto.RequiredDocumentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 제출 전 요약 응답
 *
 * @param kycId KYC 신청 ID
 * @param kycStatus KYC 상태
 * @param corporateId 법인 ID
 * @param corporateName 법인명
 * @param businessRegistrationNo 사업자등록번호
 * @param corporateRegistrationNo 법인등록번호
 * @param representativeName 대표자명
 * @param representativePhone 대표자 연락처
 * @param representativeEmail 대표자 이메일
 * @param agentName 대리인명
 * @param agentPhone 대리인 연락처
 * @param agentEmail 대리인 이메일
 * @param agentAuthorityScope 대리인 권한 범위
 * @param corporateTypeCode 법인 유형 코드
 * @param documentStoreOption 원본서류 저장 옵션
 * @param documents 업로드 문서 목록
 * @param requiredDocuments 필수서류 충족 여부 목록
 * @param submittable 제출 가능 여부
 * @param missingItems 제출 불가 사유 목록
 * @param createdAt 신청 생성일시
 * @param updatedAt 신청 수정일시
 * @param submittedAt 제출일시
 */
@Schema(description = "KYC 제출 전 요약 응답")
public record KycApplicationSummaryResponse(
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 상태", example = "DRAFT")
        String kycStatus, // KYC 상태
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "법인명", example = "주식회사 KYVC")
        String corporateName, // 법인명
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo, // 사업자등록번호
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo, // 법인등록번호
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName, // 대표자명
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String representativePhone, // 대표자 연락처
        @Schema(description = "대표자 이메일", example = "ceo@kyvc.com")
        String representativeEmail, // 대표자 이메일
        @Schema(description = "대리인명", example = "김담당")
        String agentName, // 대리인명
        @Schema(description = "대리인 연락처", example = "010-9876-5432")
        String agentPhone, // 대리인 연락처
        @Schema(description = "대리인 이메일", example = "agent@kyvc.com")
        String agentEmail, // 대리인 이메일
        @Schema(description = "대리인 권한 범위", example = "KYC 제출")
        String agentAuthorityScope, // 대리인 권한 범위
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateTypeCode, // 법인 유형 코드
        @Schema(description = "원본서류 저장 옵션", example = "STORE")
        String documentStoreOption, // 원본서류 저장 옵션
        @Schema(description = "업로드 문서 목록")
        List<KycDocumentResponse> documents, // 업로드 문서 목록
        @Schema(description = "필수서류 충족 여부 목록")
        List<RequiredDocumentResponse> requiredDocuments, // 필수서류 충족 여부 목록
        @Schema(description = "제출 가능 여부", example = "false")
        boolean submittable, // 제출 가능 여부
        @Schema(description = "제출 불가 사유 목록")
        List<KycMissingItemResponse> missingItems, // 제출 불가 사유 목록
        @Schema(description = "신청 생성일시", example = "2026-05-04T12:30:00")
        LocalDateTime createdAt, // 신청 생성일시
        @Schema(description = "신청 수정일시", example = "2026-05-04T13:00:00")
        LocalDateTime updatedAt, // 신청 수정일시
        @Schema(description = "제출일시", example = "2026-05-04T15:00:00")
        LocalDateTime submittedAt // 제출일시
) {

    public KycApplicationSummaryResponse {
        documents = documents == null ? List.of() : List.copyOf(documents);
        requiredDocuments = requiredDocuments == null ? List.of() : List.copyOf(requiredDocuments);
        missingItems = missingItems == null ? List.of() : List.copyOf(missingItems);
    }
}
