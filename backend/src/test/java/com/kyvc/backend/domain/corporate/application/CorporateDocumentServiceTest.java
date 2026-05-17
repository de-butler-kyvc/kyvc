package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.CorporateDocument;
import com.kyvc.backend.domain.corporate.repository.CorporateDocumentRepository;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorporateDocumentServiceTest {

    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE";
    private static final String REPRESENTATIVE_ID = "REPRESENTATIVE_ID";
    private static final String REPRESENTATIVE_PROOF_DOCUMENT = "REPRESENTATIVE_PROOF_DOCUMENT";

    @Mock
    private CorporateDocumentRepository corporateDocumentRepository;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private CommonCodeProvider commonCodeProvider;

    private CorporateDocumentService service;

    @BeforeEach
    void setUp() {
        service = new CorporateDocumentService(
                corporateDocumentRepository,
                documentStorage,
                new DocumentStorageProperties("./build/test-documents", 10, "pdf,jpg,jpeg,png", "application/pdf"),
                commonCodeProvider
        );
    }

    @Test
    void storeCorporateDocument_normalizesRepresentativeIdDocumentType() {
        MockMultipartFile file = new MockMultipartFile(
                "identityFile",
                "identity.pdf",
                "application/pdf",
                new byte[]{1}
        );
        when(documentStorage.storeCorporateDocument(eq(1L), eq(REPRESENTATIVE_PROOF_DOCUMENT), same(file)))
                .thenReturn(new DocumentStorage.StoredFile(
                        "identity.pdf",
                        "corporates/1/REPRESENTATIVE_PROOF_DOCUMENT/identity.pdf",
                        "application/pdf",
                        1L,
                        "hash"
                ));
        when(corporateDocumentRepository.save(any(CorporateDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CorporateDocument document = service.storeCorporateDocument(1L, REPRESENTATIVE_ID, file, 10L);

        assertThat(document.getDocumentTypeCode()).isEqualTo(REPRESENTATIVE_PROOF_DOCUMENT);
        verify(commonCodeProvider).validateEnabledCode(DOCUMENT_TYPE_GROUP, REPRESENTATIVE_PROOF_DOCUMENT);
        verify(documentStorage).storeCorporateDocument(1L, REPRESENTATIVE_PROOF_DOCUMENT, file);
    }
}
