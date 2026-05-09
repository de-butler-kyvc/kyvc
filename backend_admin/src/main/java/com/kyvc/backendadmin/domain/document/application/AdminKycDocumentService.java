package com.kyvc.backendadmin.domain.document.application;

import com.kyvc.backendadmin.domain.document.domain.KycDocument;
import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentListResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentPreviewResponse;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentQueryRepository;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentRepository;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * KYC 문서 조회와 미리보기 URL 생성 유스케이스를 담당합니다.
 *
 * <p>KYC 신청 존재 여부를 확인한 뒤 제출 문서 목록을 조회하고, 문서 단건 조회 시에는
 * 문서가 해당 KYC에 속하는지 검증한 후 원본 파일 경로를 노출하지 않는 미리보기 URL을 생성합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminKycDocumentService {

    private static final int PREVIEW_EXPIRATION_MINUTES = 10;

    private final KycApplicationRepository kycApplicationRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycDocumentQueryRepository kycDocumentQueryRepository;

    /**
     * KYC 제출 문서 목록을 조회합니다.
     *
     * <p>kycId 기준 KYC 신청 존재 여부를 먼저 검증하고, 존재하지 않으면 KYC_NOT_FOUND를 던집니다.
     * 이후 kyc_documents.kyc_id 기준으로 문서 목록을 조회하며, 문서 유형명은 가능한 경우
     * common_codes의 DOCUMENT_TYPE에서 매핑합니다.</p>
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 제출 문서 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminKycDocumentListResponse getDocuments(Long kycId) {
        validateKycExists(kycId);
        return new AdminKycDocumentListResponse(
                kycId,
                kycDocumentQueryRepository.findDocumentsByKycId(kycId)
        );
    }

    /**
     * KYC 제출 문서 미리보기 URL을 생성합니다.
     *
     * <p>kycId 기준 KYC 존재를 검증하고 documentId 기준 문서 존재를 확인합니다.
     * 조회된 문서의 kycId가 요청 kycId와 다르면 DOCUMENT_ACCESS_DENIED를 던져
     * 문서 소속을 검증합니다. 검증 후 원본 파일 경로를 응답에 포함하지 않고,
     * documentId와 만료 시각 기반의 임시 previewUrl을 생성합니다.</p>
     *
     * @param kycId KYC 신청 ID
     * @param documentId 문서 ID
     * @return KYC 제출 문서 미리보기 응답
     */
    @Transactional(readOnly = true)
    public AdminKycDocumentPreviewResponse createPreview(Long kycId, Long documentId) {
        validateKycExists(kycId);
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 문서 소속 검증: 요청 KYC의 문서가 아니면 파일 접근 권한이 없는 것으로 처리한다.
        if (!kycId.equals(document.getKycId())) {
            throw new ApiException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PREVIEW_EXPIRATION_MINUTES);
        String documentTypeName = kycDocumentQueryRepository.findDocumentTypeName(document.getDocumentTypeCode());

        // previewUrl 생성: 원본 filePath 대신 documentId와 만료 시각만 담은 임시 토큰 URL을 제공한다.
        String previewUrl = buildPreviewUrl(kycId, documentId, expiresAt);

        // 파일 경로 비노출: document.getFilePath()는 내부 검증/스트리밍 용도로만 남기고 응답 DTO에 담지 않는다.
        return new AdminKycDocumentPreviewResponse(
                document.getDocumentId(),
                document.getDocumentTypeCode(),
                documentTypeName == null ? document.getDocumentTypeCode() : documentTypeName,
                document.getFileName(),
                document.getMimeType(),
                document.getFileSize(),
                previewUrl,
                expiresAt
        );
    }

    private void validateKycExists(Long kycId) {
        kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    private String buildPreviewUrl(Long kycId, Long documentId, LocalDateTime expiresAt) {
        String tokenSource = "%d:%d:%s".formatted(kycId, documentId, expiresAt);
        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenSource.getBytes(StandardCharsets.UTF_8));
        return "/api/admin/backend/kyc/applications/%d/documents/%d/preview?token=%s"
                .formatted(kycId, documentId, token);
    }
}
