package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import com.kyvc.backend.domain.corporate.dto.CorporateResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorporateServiceTest {

    @Mock
    private CorporateRepository corporateRepository;
    @Mock
    private CorporateRepresentativeRepository corporateRepresentativeRepository;
    @Mock
    private CommonCodeProvider commonCodeProvider;
    @InjectMocks
    private CorporateService corporateService;

    @Test
    void getMyCorporateUsesRepresentativeDetailWhenLegacyColumnsAreEmpty() {
        Corporate corporate = Corporate.create(
                1L,
                "코다스페이스빌더스",
                "120-81-12395",
                "110111-3928592",
                "CORPORATION",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                KyvcEnums.CorporateStatus.ACTIVE
        );
        ReflectionTestUtils.setField(corporate, "corporateId", 10L);
        CorporateRepresentative representative = CorporateRepresentative.create(
                10L,
                "김대표",
                null,
                "KR",
                "010-1234-5678",
                "ceo@kyvc.com",
                1L
        );
        when(corporateRepository.findByUserId(1L)).thenReturn(Optional.of(corporate));
        when(corporateRepresentativeRepository.findByCorporateId(10L)).thenReturn(Optional.of(representative));

        CorporateResponse response = corporateService.getMyCorporate(1L);

        assertThat(response.representativeName()).isEqualTo("김대표");
        assertThat(response.representativePhone()).isEqualTo("010-1234-5678");
        assertThat(response.representativeEmail()).isEqualTo("ceo@kyvc.com");
    }
}
