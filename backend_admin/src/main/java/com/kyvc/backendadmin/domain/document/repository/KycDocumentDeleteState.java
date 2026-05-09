package com.kyvc.backendadmin.domain.document.repository;

public record KycDocumentDeleteState(
        Long documentId,
        Long kycId,
        String uploadStatusCode
) {
}
