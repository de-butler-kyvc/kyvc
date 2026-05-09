package com.kyvc.backendadmin.domain.issuer.application;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicySummaryResponse;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerPolicyQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class IssuerPolicyQueryServiceTest {

    @Test
    void searchRejectsStartDateAfterEndDate() {
        IssuerPolicyQueryRepository issuerPolicyQueryRepository = mock(IssuerPolicyQueryRepository.class);
        IssuerPolicyQueryService issuerPolicyQueryService = new IssuerPolicyQueryService(issuerPolicyQueryRepository);
        IssuerPolicySummaryResponse.SearchRequest request = IssuerPolicySummaryResponse.SearchRequest.of(
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

        assertThrows(ApiException.class, () -> issuerPolicyQueryService.search(request));
        verifyNoInteractions(issuerPolicyQueryRepository);
    }
}
