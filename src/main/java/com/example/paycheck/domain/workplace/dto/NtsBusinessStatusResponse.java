package com.example.paycheck.domain.workplace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class NtsBusinessStatusResponse {

    @JsonProperty("status_code")
    private String statusCode;

    private List<BusinessStatusData> data;

    public boolean isOk() {
        return "OK".equalsIgnoreCase(statusCode);
    }

    public BusinessStatusData findByBusinessNumber(String businessNumber) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        for (BusinessStatusData item : data) {
            if (item != null && businessNumber.equals(item.getBusinessNumber())) {
                return item;
            }
        }

        return null;
    }

    @Getter
    @NoArgsConstructor
    public static class BusinessStatusData {

        @JsonProperty("b_no")
        private String businessNumber;

        @JsonProperty("b_stt")
        private String businessStatus;

        @JsonProperty("b_stt_cd")
        private String businessStatusCode;

        @JsonProperty("tax_type")
        private String taxType;

        @JsonProperty("tax_type_cd")
        private String taxTypeCode;

        @JsonProperty("end_dt")
        private String endDate;

        public boolean isUnregistered() {
            return taxType != null && taxType.contains("등록되지 않은 사업자등록번호");
        }

        public boolean isClosed() {
            return businessStatus != null && businessStatus.contains("폐업");
        }
    }
}
