package com.kyvc.backendadmin.domain.document.application;

import com.kyvc.backendadmin.domain.document.domain.KycDocument;
import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentFileResponse;
import com.kyvc.backendadmin.domain.document.infrastructure.AdminDocumentStorage;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentQueryRepository;
import com.kyvc.backendadmin.domain.document.repository.KycDocumentRepository;
import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminKycDocumentServiceTest {

    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private KycDocumentQueryRepository kycDocumentQueryRepository;
    @Mock
    private AdminDocumentStorage adminDocumentStorage;

    @Test
    void loadFileUsesKycIdAndListDocumentIdTogether() {
        Long kycId = 10L;
        Long documentId = 38L;
        KycDocument document = mock(KycDocument.class);
        when(kycApplicationRepository.findById(kycId)).thenReturn(Optional.of(mock(KycApplication.class)));
        when(kycDocumentRepository.findByKycIdAndDocumentId(kycId, documentId)).thenReturn(Optional.of(document));
        when(document.getFilePath()).thenReturn("10/BUSINESS_REGISTRATION/document.pdf");
        when(document.getFileName()).thenReturn("document.pdf");
        when(document.getMimeType()).thenReturn("application/pdf");
        when(adminDocumentStorage.load("10/BUSINESS_REGISTRATION/document.pdf"))
                .thenReturn(new AdminDocumentStorage.StoredContent(
                        new ByteArrayResource("pdf".getBytes()),
                        3L,
                        Path.of("document.pdf")
                ));
        AdminKycDocumentService service = service();

        AdminKycDocumentFileResponse response = service.loadFile(kycId, documentId);

        assertEquals("document.pdf", response.fileName());
        assertEquals("application/pdf", response.mimeType());
        assertEquals(3L, response.fileSize());
        verify(kycDocumentRepository).findByKycIdAndDocumentId(kycId, documentId);
        verify(kycDocumentRepository, never()).findById(documentId);
    }

    @Test
    void loadFileRejectsDocumentOutsideKycAsNotFound() {
        Long kycId = 10L;
        Long documentId = 38L;
        when(kycApplicationRepository.findById(kycId)).thenReturn(Optional.of(mock(KycApplication.class)));
        when(kycDocumentRepository.findByKycIdAndDocumentId(kycId, documentId)).thenReturn(Optional.empty());
        AdminKycDocumentService service = service();

        ApiException exception = assertThrows(ApiException.class, () -> service.loadFile(kycId, documentId));

        assertEquals(ErrorCode.DOCUMENT_NOT_FOUND, exception.getErrorCode());
        verify(kycDocumentRepository).findByKycIdAndDocumentId(kycId, documentId);
        verify(kycDocumentRepository, never()).findById(documentId);
    }

    private AdminKycDocumentService service() {
        return new AdminKycDocumentService(
                kycApplicationRepository,
                kycDocumentRepository,
                kycDocumentQueryRepository,
                adminDocumentStorage
        );
    }
}
