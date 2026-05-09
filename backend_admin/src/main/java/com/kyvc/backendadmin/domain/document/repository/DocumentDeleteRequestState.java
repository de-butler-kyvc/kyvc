package com.kyvc.backendadmin.domain.document.repository;

import java.time.LocalDateTime;

public record DocumentDeleteRequestState(
        Long requestId,
        Long documentId,
        String requestStatusCode,
        Long processedByAdminId,
        String processedReason,
        LocalDateTime processedAt
) {
}
