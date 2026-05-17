package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import com.kyvc.backend.domain.corporate.dto.CorporateBasicInfoRequest;
import com.kyvc.backend.domain.corporate.dto.CorporateCreateRequest;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void createCorporateStoresEstablishedDateAndBusinessType() {
        CorporateCreateRequest request = new CorporateCreateRequest(
                "코다스페이스빌더스",
                "120-81-12395",
                "110111-3928592",
                null,
                LocalDate.of(2020, 1, 15),
                "02-1234-5678",
                "서울특별시 강남구 테헤란로 1",
                "https://kyvc.com",
                "소프트웨어 개발 및 공급업"
        );
        when(corporateRepository.save(any(Corporate.class))).thenAnswer(invocation -> {
            Corporate corporate = invocation.getArgument(0);
            ReflectionTestUtils.setField(corporate, "corporateId", 10L);
            return corporate;
        });
        when(corporateRepresentativeRepository.findByCorporateId(10L)).thenReturn(Optional.empty());

        CorporateResponse response = corporateService.createCorporate(1L, request);

        assertThat(response.establishedDate()).isEqualTo(LocalDate.of(2020, 1, 15));
        assertThat(response.businessType()).isEqualTo("소프트웨어 개발 및 공급업");
    }

    @Test
    void updateBasicInfoStoresEstablishedDateAndBusinessType() {
        Corporate corporate = Corporate.create(
                1L,
                "코다스페이스빌더스",
                "120-81-12395",
                "110111-3928592",
                null,
                LocalDate.of(2020, 1, 15),
                null,
                null,
                null,
                null,
                "서울특별시 강남구 테헤란로 1",
                "https://kyvc.com",
                "기존 업종",
                KyvcEnums.CorporateStatus.ACTIVE
        );
        ReflectionTestUtils.setField(corporate, "corporateId", 10L);
        CorporateBasicInfoRequest request = new CorporateBasicInfoRequest(
                "코다스페이스빌더스",
                "120-81-12395",
                "110111-3928592",
                null,
                LocalDate.of(2021, 2, 20),
                "02-9999-0000",
                "서울특별시 서초구 서초대로 1",
                "https://updated.kyvc.com",
                "정보통신업"
        );
        when(corporateRepository.findById(10L)).thenReturn(Optional.of(corporate));
        when(corporateRepository.save(any(Corporate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(corporateRepresentativeRepository.findByCorporateId(10L)).thenReturn(Optional.empty());

        CorporateResponse response = corporateService.updateBasicInfo(1L, 10L, request);

        assertThat(response.establishedDate()).isEqualTo(LocalDate.of(2021, 2, 20));
        assertThat(response.businessType()).isEqualTo("정보통신업");
    }

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
