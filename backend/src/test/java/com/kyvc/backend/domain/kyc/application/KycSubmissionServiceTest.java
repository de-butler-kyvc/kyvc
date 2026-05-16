package com.kyvc.backend.domain.kyc.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.exception.CoreAiReviewException;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateAgentRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
import com.kyvc.backend.domain.document.application.DocumentRequirementValidationService;
import com.kyvc.backend.domain.document.application.RequiredDocumentPolicyProvider;
import com.kyvc.backend.domain.document.domain.DocumentRequirementValidationResult;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorage;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.KycSubmitResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycSubmissionServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long KYC_ID = 20L;
    private static final Long CORPORATE_ID = 30L;
    private static final String CORPORATE_TYPE_CODE = "CORPORATION";
    private static final String AI_ENDPOINT = "/ai-assessment/assessments/llm-primary";

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
    private DocumentRequirementValidationService documentRequirementValidationService;
    @Mock
    private DocumentStorage documentStorage;
    @Mock
    private CoreRequestService coreRequestService;
    @Mock
    private CoreAdapter coreAdapter;
    @Mock
    private LogEventLogger logEventLogger;

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
                documentRequirementValidationService,
                documentStorage,
                coreRequestService,
                coreAdapter,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    @Test
    void submit_returnsManualReviewWhenCoreAiReviewTimeout() {
        CoreRequest coreRequest = prepareSubmittableKyc();
        doThrow(coreAiReviewException(
                coreRequest.getCoreRequestId(),
                CoreAiReviewException.FailureType.TIMEOUT,
                ErrorCode.CORE_API_TIMEOUT,
                "Core AI review request timed out"
        )).when(coreAdapter).requestAiReview(any(CoreAiReviewRequest.class));

        KycSubmitResponse response = service.submit(USER_ID, KYC_ID);

        assertManualReviewFallback(response);
        verify(coreRequestService).markFailed(coreRequest.getCoreRequestId(), "CORE_AI_REVIEW_TIMEOUT");
        verify(coreRequestService, never()).markTimeout(any(), any());
    }

    @Test
    void submit_returnsManualReviewWhenCoreAiReviewInvalidResponse() {
        CoreRequest coreRequest = prepareSubmittableKyc();
        doThrow(coreAiReviewException(
                coreRequest.getCoreRequestId(),
                CoreAiReviewException.FailureType.INVALID_RESPONSE,
                ErrorCode.CORE_API_RESPONSE_INVALID,
                "Core AI review response is not valid JSON"
        )).when(coreAdapter).requestAiReview(any(CoreAiReviewRequest.class));

        KycSubmitResponse response = service.submit(USER_ID, KYC_ID);

        assertManualReviewFallback(response);
        verify(coreRequestService).markFailed(coreRequest.getCoreRequestId(), "CORE_AI_REVIEW_INVALID_RESPONSE");
        verify(coreRequestService, never()).markTimeout(any(), any());
    }

    private CoreRequest prepareSubmittableKyc() {
        KycApplication kycApplication = KycApplication.createDraft(CORPORATE_ID, USER_ID, CORPORATE_TYPE_CODE);
        ReflectionTestUtils.setField(kycApplication, "kycId", KYC_ID);
        kycApplication.changeDocumentStoreOption(KyvcEnums.OriginalDocumentStoreOption.STORE);

        Corporate corporate = Corporate.create(
                USER_ID,
                "KYVC Corp",
                "1234567890",
                "1101111234567",
                CORPORATE_TYPE_CODE,
                null,
                "0212345678",
                "대표자",
                "01012345678",
                "rep@example.com",
                "서울시 강남구",
                null,
                "IT",
                KyvcEnums.CorporateStatus.ACTIVE
        );
        ReflectionTestUtils.setField(corporate, "corporateId", CORPORATE_ID);

        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.AI_REVIEW,
                KyvcEnums.CoreTargetType.KYC_APPLICATION,
                KYC_ID,
                null
        );

        when(kycApplicationRepository.findById(KYC_ID)).thenReturn(Optional.of(kycApplication));
        when(corporateRepository.findById(CORPORATE_ID)).thenReturn(Optional.of(corporate));
        when(corporateRepresentativeRepository.findByCorporateId(CORPORATE_ID)).thenReturn(Optional.empty());
        when(corporateAgentRepository.findByCorporateId(CORPORATE_ID)).thenReturn(List.of());
        when(kycDocumentRepository.findByKycId(KYC_ID)).thenReturn(List.of());
        when(requiredDocumentPolicyProvider.getRequiredDocuments(CORPORATE_TYPE_CODE)).thenReturn(List.of());
        when(documentRequirementValidationService.validate(CORPORATE_TYPE_CODE, Set.of(), false))
                .thenReturn(new DocumentRequirementValidationResult(true, List.of(), List.of()));
        when(coreRequestService.createAiReviewRequest(KYC_ID, null)).thenReturn(coreRequest);
        when(kycApplicationRepository.save(any(KycApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        return coreRequest;
    }

    private CoreAiReviewException coreAiReviewException(
            String coreRequestId, // Core 요청 ID
            CoreAiReviewException.FailureType failureType, // 실패 유형
            ErrorCode errorCode, // 오류 코드
            String message // 실패 메시지
    ) {
        return new CoreAiReviewException(
                errorCode,
                message,
                null,
                AI_ENDPOINT,
                coreRequestId,
                KYC_ID,
                CORPORATE_ID,
                failureType,
                null,
                null,
                null,
                1000L,
                300
        );
    }

    private void assertManualReviewFallback(
            KycSubmitResponse response // 제출 응답
    ) {
        assertThat(response.status()).isEqualTo(KyvcEnums.KycStatus.MANUAL_REVIEW.name());
        assertThat(response.aiReviewStatus()).isEqualTo(KyvcEnums.AiReviewStatus.FAILED.name());
        assertThat(response.nextActionCode()).isEqualTo(KyvcEnums.KycCompletionAction.WAIT_MANUAL_REVIEW.name());
        assertThat(response.submittable()).isFalse();
    }
}
