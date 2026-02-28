package com.example.paycheck.domain.workplace.service;

import com.example.paycheck.common.exception.BadRequestException;
import com.example.paycheck.common.exception.ErrorCode;
import com.example.paycheck.domain.workplace.dto.NtsBusinessStatusRequest;
import com.example.paycheck.domain.workplace.dto.NtsBusinessStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessNumberVerificationService {

    private final NtsBusinessStatusClient ntsBusinessStatusClient;

    public void verifyBusinessNumber(String businessNumber) {
        String normalized = normalize(businessNumber);
        if (normalized.length() != 10) {
            throw new BadRequestException(ErrorCode.INVALID_BUSINESS_NUMBER, "사업자 등록번호 형식이 올바르지 않습니다.");
        }

        if (!ntsBusinessStatusClient.isConfigured()) {
            throw new BadRequestException(
                    ErrorCode.BUSINESS_NUMBER_VERIFICATION_FAILED,
                    "사업자 등록번호 검증 API 설정이 누락되었습니다."
            );
        }

        NtsBusinessStatusResponse response = ntsBusinessStatusClient.fetchStatus(
                NtsBusinessStatusRequest.builder()
                        .businessNumbers(List.of(normalized))
                        .build()
        );

        if (response == null || !response.isOk()) {
            throw new BadRequestException(
                    ErrorCode.BUSINESS_NUMBER_VERIFICATION_FAILED,
                    "사업자 등록번호 검증에 실패했습니다."
            );
        }

        NtsBusinessStatusResponse.BusinessStatusData status = response.findByBusinessNumber(normalized);
        if (status == null) {
            throw new BadRequestException(ErrorCode.INVALID_BUSINESS_NUMBER, "유효하지 않은 사업자 등록번호입니다.");
        }

        if (status.isUnregistered()) {
            throw new BadRequestException(ErrorCode.INVALID_BUSINESS_NUMBER, "유효하지 않은 사업자 등록번호입니다.");
        }

        if (status.isClosed()) {
            throw new BadRequestException(ErrorCode.INVALID_BUSINESS_NUMBER, "폐업된 사업자 등록번호입니다.");
        }
    }

    private String normalize(String businessNumber) {
        if (businessNumber == null) {
            return "";
        }
        return businessNumber.replaceAll("[^0-9]", "");
    }
}
