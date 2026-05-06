package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// KYC 필수서류 정책 Provider
@Component
@RequiredArgsConstructor
public class RequiredDocumentPolicyProvider {

    private final DocumentStorageProperties documentStorageProperties;

    // 법인 유형 기준 필수서류 정책 목록 조회
    public List<RequiredDocumentPolicy> getRequiredDocuments(
            String corporateTypeCode // 법인 유형 코드
    ) {
        return List.of(
                createPolicy(
                        "BUSINESS_REGISTRATION",
                        "사업자등록증",
                        "사업자등록증을 업로드한다."
                ),
                createPolicy(
                        "CORPORATE_REGISTRATION",
                        "법인등기부등본",
                        "법인등기부등본을 업로드한다."
                ),
                createPolicy(
                        "CORPORATE_SEAL_CERTIFICATE",
                        "법인인감증명서",
                        "3개월 이내 발급된 법인인감증명서를 업로드한다."
                ),
                createPolicy(
                        "SHAREHOLDER_LIST",
                        "주주명부",
                        "3개월 이내 발급된 주주명부를 업로드한다."
                )
        );
    }

    // 법인 유형 기준 필수 문서 유형 코드 목록 조회
    public List<String> getRequiredDocumentTypeCodes(
            String corporateTypeCode // 법인 유형 코드
    ) {
        return getRequiredDocuments(corporateTypeCode).stream()
                .map(RequiredDocumentPolicy::documentTypeCode)
                .toList();
    }

    // 법인 유형 기준 필수 문서 여부 조회
    public boolean isRequiredDocument(
            String corporateTypeCode, // 법인 유형 코드
            String documentTypeCode // 문서 유형 코드
    ) {
        return getRequiredDocumentTypeCodes(corporateTypeCode).contains(documentTypeCode);
    }

    // 필수서류 정책 생성
    private RequiredDocumentPolicy createPolicy(
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형 표시명
            String description // 제출 안내 문구
    ) {
        return new RequiredDocumentPolicy(
                documentTypeCode,
                documentTypeName,
                true,
                description,
                documentStorageProperties.getAllowedExtensions(),
                documentStorageProperties.getMaxFileSizeMb()
        );
    }

    // 필수서류 정책 데이터
    public record RequiredDocumentPolicy(
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형 표시명
            boolean required, // 필수 여부
            String description, // 제출 안내 문구
            List<String> allowedExtensions, // 허용 확장자 목록
            int maxFileSizeMb // 최대 파일 크기 MB
    ) {

        public RequiredDocumentPolicy {
            allowedExtensions = allowedExtensions == null ? List.of() : List.copyOf(allowedExtensions);
        }
    }
}
