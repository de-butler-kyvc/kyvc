package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialIssuanceServiceTest {

    @Mock
    private CredentialRepository credentialRepository;
    @Mock
    private CredentialRequestRepository credentialRequestRepository;
    @Mock
    private CredentialStatusHistoryRepository credentialStatusHistoryRepository;
    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private CoreRequestService coreRequestService;
    @Mock
    private CoreAdapter coreAdapter;
    @Mock
    private LogEventLogger logEventLogger;

    @Captor
    private ArgumentCaptor<CoreVcIssuanceRequest> coreVcIssuanceRequestCaptor;

    private CredentialIssuanceService credentialIssuanceService;

    @BeforeEach
    void setUp() {
        CoreProperties coreProperties = new CoreProperties();
        coreProperties.setDevSeedEnabled(true);
        credentialIssuanceService = new CredentialIssuanceService(
                credentialRepository,
                credentialRequestRepository,
                credentialStatusHistoryRepository,
                kycApplicationRepository,
                coreRequestService,
                coreAdapter,
                coreProperties,
                new ObjectMapper().findAndRegisterModules(),
                logEventLogger
        );
    }

    @Test
    void issueKycCredentialForUser_requestsCoreWithDcSdJwtFormat() {
        KycApplication kycApplication = KycApplication.createDraft(20L, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", 10L);
        kycApplication.approveForDevTest(LocalDateTime.now());
        CoreRequest coreRequest = CoreRequest.create(
                KyvcEnums.CoreRequestType.VC_ISSUE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                100L,
                null
        );
        CoreVcIssuanceResponse coreResponse = new CoreVcIssuanceResponse(
                coreRequest.getCoreRequestId(),
                KyvcEnums.CredentialStatus.VALID.name(),
                "issued",
                LocalDateTime.now(),
                "credential-external-id",
                CoreMockSeedData.DEV_CREDENTIAL_TYPE,
                CoreMockSeedData.DEV_ISSUER_DID,
                "dc+sd-jwt",
                null,
                "header.payload.signature~disclosure-001~",
                "vc-hash",
                "tx-hash",
                "status-id",
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(1),
                null
        );

        when(credentialRepository.findLatestByKycId(10L)).thenReturn(Optional.empty());
        when(credentialRepository.save(any(Credential.class))).thenAnswer(invocation -> {
            Credential credential = invocation.getArgument(0);
            if (credential.getCredentialId() == null) {
                ReflectionTestUtils.setField(credential, "credentialId", 100L);
            }
            return credential;
        });
        when(credentialRequestRepository.save(any(CredentialRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coreRequestService.createVcIssuanceRequest(eq(100L), isNull())).thenReturn(coreRequest);
        when(coreRequestService.updateRequestPayloadJson(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreRequestService.markRunning(coreRequest.getCoreRequestId())).thenReturn(coreRequest);
        when(coreRequestService.markSuccess(eq(coreRequest.getCoreRequestId()), any())).thenReturn(coreRequest);
        when(coreAdapter.requestVcIssuance(any())).thenReturn(coreResponse);
        when(credentialStatusHistoryRepository.save(any(CredentialStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Credential credential = credentialIssuanceService.issueKycCredentialForUser(kycApplication, 1L);

        verify(coreAdapter).requestVcIssuance(coreVcIssuanceRequestCaptor.capture());
        assertThat(coreVcIssuanceRequestCaptor.getValue().format()).isEqualTo("dc+sd-jwt");
        assertThat(credential.getVcFormat()).isEqualTo("dc+sd-jwt");
    }
}
