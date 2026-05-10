package com.kyvc.backendadmin.domain.document.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.domain.document.repository.DocumentDeleteRequestQueryRepository;
import com.kyvc.backendadmin.domain.document.repository.DocumentDeleteRequestRepository;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DocumentDeleteRequestServiceTest {

    @Test
    void searchRejectsStartDateAfterEndDate() {
        DocumentDeleteRequestRepository documentDeleteRequestRepository = mock(DocumentDeleteRequestRepository.class);
        DocumentDeleteRequestQueryRepository documentDeleteRequestQueryRepository = mock(DocumentDeleteRequestQueryRepository.class);
        DocumentDeleteRequestService service = new DocumentDeleteRequestService(
                documentDeleteRequestRepository,
                documentDeleteRequestQueryRepository,
                mock(KycDocumentRepository.class),
                mock(AuthTokenRepository.class),
                mock(AuditLogWriter.class)
        );
        DocumentDeleteRequestSearchRequest request = DocumentDeleteRequestSearchRequest.of(
                0,
                20,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 1)
        );

        assertThrows(ApiException.class, () -> service.search(request));
        verifyNoInteractions(documentDeleteRequestRepository, documentDeleteRequestQueryRepository);
    }
}
