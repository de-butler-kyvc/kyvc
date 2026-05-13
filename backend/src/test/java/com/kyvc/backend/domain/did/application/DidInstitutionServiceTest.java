package com.kyvc.backend.domain.did.application;

import com.kyvc.backend.domain.did.domain.DidInstitution;
import com.kyvc.backend.domain.did.dto.DidInstitutionResponse;
import com.kyvc.backend.domain.did.repository.DidInstitutionRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DidInstitutionServiceTest {

    @Mock
    private DidInstitutionRepository didInstitutionRepository;

    private DidInstitutionService didInstitutionService;

    @BeforeEach
    void setUp() {
        didInstitutionService = new DidInstitutionService(didInstitutionRepository);
    }

    @Test
    void getInstitution_returnsActiveInstitution() {
        DidInstitution didInstitution = mock(DidInstitution.class);
        when(didInstitution.getDid()).thenReturn("did:xrpl:1:rIssuer");
        when(didInstitution.getInstitutionName()).thenReturn("KYvC Issuer");
        when(didInstitution.getStatusCode()).thenReturn(KyvcEnums.DidInstitutionStatus.ACTIVE);
        when(didInstitutionRepository.findActiveByDid("did:xrpl:1:rIssuer")).thenReturn(Optional.of(didInstitution));

        DidInstitutionResponse response = didInstitutionService.getInstitution("did:xrpl:1:rIssuer");

        assertThat(response.did()).isEqualTo("did:xrpl:1:rIssuer");
        assertThat(response.institutionName()).isEqualTo("KYvC Issuer");
        assertThat(response.status()).isEqualTo(KyvcEnums.DidInstitutionStatus.ACTIVE.name());
    }

    @Test
    void getInstitution_throwsNotFoundWhenActiveMappingMissing() {
        when(didInstitutionRepository.findActiveByDid("did:xrpl:1:rIssuer")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> didInstitutionService.getInstitution("did:xrpl:1:rIssuer"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DID_INSTITUTION_NOT_FOUND);
    }

    @Test
    void getInstitution_throwsInvalidDidWithoutRepositoryCall() {
        assertThatThrownBy(() -> didInstitutionService.getInstitution("invalid"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DID);

        verifyNoInteractions(didInstitutionRepository);
    }
}
