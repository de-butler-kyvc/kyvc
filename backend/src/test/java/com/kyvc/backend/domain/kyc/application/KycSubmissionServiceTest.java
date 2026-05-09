package com.kyvc.backend.domain.kyc.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateAgentRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
import com.kyvc.backend.domain.document.application.RequiredDocumentPolicyProvider;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.KycSubmitResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycSubmissionServiceTest {

    @Mock
    private KycApplicationRepository kycApplicationRepository;

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private CorporateRepresentativeRepository corporateRepresentativeRepository;

    @Mock
    private CorporateAgentRepository corporateAgentRepository;

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;

    @Mock
    private DocumentStorage documentStorage;

    @Mock
    private CoreRequestService coreRequestService;

    @Mock
    private CoreAdapter coreAdapter;

    @Mock
    private LogEventLogger logEventLogger;

    @Captor
    private ArgumentCaptor<String> requestPayloadCaptor;

    private KycSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new KycSubmissionService(
                kycApplicationRepository,
                corporateRepository,
                corporateRepresentativeRepository,
                corporateAgentRepository,
                kycDocumentRepository,
                requiredDocumentPolicyProvider,
                documentStorage,
                coreRequestService,
                coreAdapter,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    @Test
    void submit_callsCoreSynchronouslyAndReturnsFinalStatus() {
        KycApplication kycApplication = createDraftKyc();
        Corporate corporate = createCorporate();
        KycDocument document = createDocument("BUSINESS_REGISTRATION");
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.AI_REVIEW,
                KyvcEnums.CoreTargetType.KYC_APPLICATION,
                100L,
                null
        );

        mockSubmittableKyc(kycApplication, corporate, document);
        when(coreRequestService.createAiReviewRequest(100L, null)).thenReturn(coreRequest);
        when(coreAdapter.requestAiReview(any(CoreAiReviewRequest.class))).thenAnswer(invocation -> {
            CoreAiReviewRequest request = invocation.getArgument(0);
            return new CoreAiReviewResponse(
                    request.coreRequestId(),
                    KyvcEnums.AiReviewStatus.SUCCESS.name(),
                    "MANUAL_REVIEW_REQUIRED",
                    "assessment-1",
                    BigDecimal.valueOf(0.88),
                    "AI review completed",
                    LocalDateTime.now()
            );
        });
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycSubmitResponse response = service.submit(1L, 100L);

        verify(coreRequestService).markRunning(coreRequest.getCoreRequestId());
        verify(coreRequestService).updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), requestPayloadCaptor.capture());
        verify(coreRequestService).markSuccess(eq(coreRequest.getCoreRequestId()), any());
        verify(coreAdapter).requestAiReview(any(CoreAiReviewRequest.class));

        assertThat(requestPayloadCaptor.getValue()).contains("BUSINESS_REGISTRATION");
        assertThat(response.status()).isEqualTo(KyvcEnums.KycStatus.MANUAL_REVIEW.name());
        assertThat(response.status()).isNotEqualTo(KyvcEnums.KycStatus.AI_REVIEWING.name());
        assertThat(response.aiReviewStatus()).isEqualTo(KyvcEnums.AiReviewStatus.SUCCESS.name());
        assertThat(response.nextActionCode()).isEqualTo(KyvcEnums.KycCompletionAction.WAIT_MANUAL_REVIEW.name());
    }

    @Test
    void submit_marksTimeoutAndReturnsManualReviewWhenCoreTimeout() {
        KycApplication kycApplication = createDraftKyc();
        Corporate corporate = createCorporate();
        KycDocument document = createDocument("BUSINESS_REGISTRATION");
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.AI_REVIEW,
                KyvcEnums.CoreTargetType.KYC_APPLICATION,
                100L,
                null
        );

        mockSubmittableKyc(kycApplication, corporate, document);
        when(coreRequestService.createAiReviewRequest(100L, null)).thenReturn(coreRequest);
        when(coreAdapter.requestAiReview(any(CoreAiReviewRequest.class)))
                .thenThrow(new ApiException(ErrorCode.CORE_API_TIMEOUT));
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KycSubmitResponse response = service.submit(1L, 100L);

        verify(coreRequestService).markRunning(coreRequest.getCoreRequestId());
        verify(coreRequestService).markTimeout(eq(coreRequest.getCoreRequestId()), any());

        assertThat(response.status()).isEqualTo(KyvcEnums.KycStatus.MANUAL_REVIEW.name());
        assertThat(response.aiReviewStatus()).isEqualTo(KyvcEnums.AiReviewStatus.FAILED.name());
        assertThat(response.nextActionCode()).isEqualTo(KyvcEnums.KycCompletionAction.WAIT_MANUAL_REVIEW.name());
    }

    private void mockSubmittableKyc(
            KycApplication kycApplication, // KYC 신청
            Corporate corporate, // 법인
            KycDocument document // KYC 문서
    ) {
        when(kycApplicationRepository.findById(100L)).thenReturn(Optional.of(kycApplication));
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(corporateRepresentativeRepository.findByCorporateId(10L)).thenReturn(Optional.empty());
        when(corporateAgentRepository.findByCorporateId(10L)).thenReturn(List.of());
        when(kycDocumentRepository.findByKycId(100L)).thenReturn(List.of(document));
        when(requiredDocumentPolicyProvider.getRequiredDocuments("CORPORATION"))
                .thenReturn(List.of(new RequiredDocumentPolicyProvider.RequiredDocumentPolicy(
                        "BUSINESS_REGISTRATION",
                        "사업자등록증",
                        true,
                        "사업자등록증",
                        List.of("pdf"),
                        10
                )));
        when(documentStorage.load("kyc/100/business.pdf")).thenReturn(new DocumentStorage.StoredContent(
                new ByteArrayResource("document".getBytes()),
                8L
        ));
    }

    private KycApplication createDraftKyc() {
        KycApplication kycApplication = KycApplication.createDraft(10L, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", 100L);
        kycApplication.changeDocumentStoreOption(KyvcEnums.OriginalDocumentStoreOption.STORE);
        return kycApplication;
    }

    private Corporate createCorporate() {
        Corporate corporate = Corporate.create(
                1L,
                "KYVC Corp",
                "123-45-67890",
                "110111-1234567",
                "CORPORATION",
                null,
                null,
                "대표자",
                "010-0000-0000",
                "rep@test.com",
                "서울",
                null,
                "IT",
                KyvcEnums.CorporateStatus.ACTIVE
        );
        ReflectionTestUtils.setField(corporate, "corporateId", 10L);
        return corporate;
    }

    private KycDocument createDocument(String documentTypeCode) {
        KycDocument document = KycDocument.createUploaded(
                100L,
                documentTypeCode,
                "business.pdf",
                "kyc/100/business.pdf",
                "application/pdf",
                8L,
                "doc-hash",
                KyvcEnums.UploadActorType.USER,
                1L
        );
        ReflectionTestUtils.setField(document, "documentId", 500L);
        return document;
    }
}
