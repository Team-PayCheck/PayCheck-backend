package com.example.paycheck.domain.workplace.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.domain.workplace.dto.NtsBusinessStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessNumberVerificationService 테스트")
class BusinessNumberVerificationServiceTest {

    @Mock
    private NtsBusinessStatusClient ntsBusinessStatusClient;

    @InjectMocks
    private BusinessNumberVerificationService businessNumberVerificationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("사업자번호 검증 실패 - API 미설정")
    void verifyBusinessNumber_Fail_NotConfigured() {
        when(ntsBusinessStatusClient.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> businessNumberVerificationService.verifyBusinessNumber("123-45-67890"))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_NUMBER_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("사업자번호 검증 실패 - status_code 비정상")
    void verifyBusinessNumber_Fail_StatusNotOk() throws Exception {
        when(ntsBusinessStatusClient.isConfigured()).thenReturn(true);
        when(ntsBusinessStatusClient.fetchStatus(any())).thenReturn(
                parseResponse("{\"status_code\":\"ERROR\",\"data\":[]}")
        );

        assertThatThrownBy(() -> businessNumberVerificationService.verifyBusinessNumber("123-45-67890"))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_NUMBER_VERIFICATION_FAILED);
    }

    @Test
    @DisplayName("사업자번호 검증 실패 - 등록되지 않은 번호")
    void verifyBusinessNumber_Fail_Unregistered() throws Exception {
        when(ntsBusinessStatusClient.isConfigured()).thenReturn(true);
        when(ntsBusinessStatusClient.fetchStatus(any())).thenReturn(
                parseResponse("{\"status_code\":\"OK\",\"data\":[{\"b_no\":\"1234567890\",\"tax_type\":\"국세청에 등록되지 않은 사업자등록번호입니다.\"}]}")
        );

        assertThatThrownBy(() -> businessNumberVerificationService.verifyBusinessNumber("123-45-67890"))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_BUSINESS_NUMBER);
    }

    @Test
    @DisplayName("사업자번호 검증 실패 - 폐업 사업자")
    void verifyBusinessNumber_Fail_Closed() throws Exception {
        when(ntsBusinessStatusClient.isConfigured()).thenReturn(true);
        when(ntsBusinessStatusClient.fetchStatus(any())).thenReturn(
                parseResponse("{\"status_code\":\"OK\",\"data\":[{\"b_no\":\"1234567890\",\"b_stt\":\"폐업자\"}]}")
        );

        assertThatThrownBy(() -> businessNumberVerificationService.verifyBusinessNumber("123-45-67890"))
                .isInstanceOf(BadRequestException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_BUSINESS_NUMBER);
    }

    @Test
    @DisplayName("사업자번호 검증 성공")
    void verifyBusinessNumber_Success() throws Exception {
        when(ntsBusinessStatusClient.isConfigured()).thenReturn(true);
        when(ntsBusinessStatusClient.fetchStatus(any())).thenReturn(
                parseResponse("{\"status_code\":\"OK\",\"data\":[{\"b_no\":\"1234567890\",\"b_stt\":\"계속사업자\",\"tax_type\":\"일반과세자\"}]}")
        );

        businessNumberVerificationService.verifyBusinessNumber("123-45-67890");
    }

    private NtsBusinessStatusResponse parseResponse(String json) throws Exception {
        return objectMapper.readValue(json, NtsBusinessStatusResponse.class);
    }
}
