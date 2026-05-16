package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.KycStartRequest;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycApplicationServiceTest {

    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private CorporateRepository corporateRepository;
    @Mock
    private CommonCodeProvider commonCodeProvider;
    @InjectMocks
    private KycApplicationService kycApplicationService;

    @Test
    void startKycAllowsMultipleApplicationsForSameUser() {
        Long userId = 1L;
        Corporate corporate = org.mockito.Mockito.mock(Corporate.class);
        when(corporate.getCorporateId()).thenReturn(10L);
        when(corporateRepository.findByUserId(userId)).thenReturn(Optional.of(corporate));
        when(kycApplicationRepository.save(any(KycApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KycStartRequest request = new KycStartRequest("CORPORATION");
        kycApplicationService.startKyc(userId, request);
        kycApplicationService.startKyc(userId, request);

        ArgumentCaptor<KycApplication> captor = ArgumentCaptor.forClass(KycApplication.class);
        verify(kycApplicationRepository, times(2)).save(captor.capture());
        verify(kycApplicationRepository, never()).existsInProgressByApplicantUserId(userId);
        verify(commonCodeProvider, times(2)).validateEnabledCode(eq("CORPORATE_TYPE"), eq("CORPORATION"));

        List<KycApplication> savedApplications = captor.getAllValues();
        assertThat(savedApplications).hasSize(2);
        assertThat(savedApplications.get(0)).isNotSameAs(savedApplications.get(1));
        assertThat(savedApplications)
                .allSatisfy(application -> {
                    assertThat(application.getCorporateId()).isEqualTo(10L);
                    assertThat(application.getApplicantUserId()).isEqualTo(userId);
                    assertThat(application.isDraft()).isTrue();
                });
    }

    @Test
    void startKycNormalizesJointStockCompanyAliasToCorporation() {
        Long userId = 1L;
        Corporate corporate = org.mockito.Mockito.mock(Corporate.class);
        when(corporate.getCorporateId()).thenReturn(10L);
        when(corporateRepository.findByUserId(userId)).thenReturn(Optional.of(corporate));
        when(kycApplicationRepository.save(any(KycApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        kycApplicationService.startKyc(userId, new KycStartRequest("JOINT_STOCK_COMPANY"));

        ArgumentCaptor<KycApplication> captor = ArgumentCaptor.forClass(KycApplication.class);
        verify(kycApplicationRepository).save(captor.capture());
        verify(commonCodeProvider).validateEnabledCode(eq("CORPORATE_TYPE"), eq("CORPORATION"));
        assertThat(captor.getValue().getCorporateTypeCode()).isEqualTo("CORPORATION");
    }
}
