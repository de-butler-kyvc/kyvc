package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialRepositoryImplTest {

    @Mock
    private CredentialJpaRepository credentialJpaRepository;

    @Captor
    private ArgumentCaptor<LocalDateTime> expiresAtCaptor;

    private CredentialRepositoryImpl credentialRepository;

    @BeforeEach
    void setUp() {
        credentialRepository = new CredentialRepositoryImpl(credentialJpaRepository);
    }

    @Test
    void findVpEligibleCredentialsByCorporateId_usesWalletSavedValidAndNotExpiredFilter() {
        LocalDateTime beforeCall = LocalDateTime.now();
        when(credentialJpaRepository
                .findAllByCorporateIdAndWalletSavedYnAndCredentialStatusAndExpiresAtGreaterThanEqualOrderByIssuedAtDesc(
                        eq(10L),
                        eq(KyvcEnums.Yn.Y.name()),
                        eq(KyvcEnums.CredentialStatus.VALID),
                        expiresAtCaptor.capture()
                ))
                .thenReturn(List.of());

        List<Credential> result = credentialRepository.findVpEligibleCredentialsByCorporateId(10L);

        LocalDateTime afterCall = LocalDateTime.now();
        assertThat(result).isEmpty();
        assertThat(expiresAtCaptor.getValue()).isBetween(beforeCall.minusNanos(1), afterCall.plusNanos(1));
        verify(credentialJpaRepository)
                .findAllByCorporateIdAndWalletSavedYnAndCredentialStatusAndExpiresAtGreaterThanEqualOrderByIssuedAtDesc(
                        eq(10L),
                        eq(KyvcEnums.Yn.Y.name()),
                        eq(KyvcEnums.CredentialStatus.VALID),
                        eq(expiresAtCaptor.getValue())
                );
    }
}
