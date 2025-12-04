package com.example.wagemanager.domain.contract.dto;

import com.example.wagemanager.domain.contract.entity.WorkerContract;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ContractDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContractCreateRequest")
    public static class CreateRequest {
        @NotBlank(message = "근로자 코드는 필수입니다.")
        private String workerCode;

        @NotNull(message = "시급은 필수입니다.")
        @DecimalMin(value = "10030.0", inclusive = true, message = "시급은 10,030 이상이어야 합니다.")
        private BigDecimal hourlyWage;

        @NotNull(message = "근무 요일은 필수입니다.")
        @Size(min = 1, message = "최소 1개의 근무 요일을 선택해야 합니다.")
        private List<@Min(value = 1, message = "근무 요일은 1~7 사이여야 합니다.") @Max(value = 7, message = "근무 요일은 1~7 사이여야 합니다.") Integer> workDays;

        @NotNull(message = "계약 시작일은 필수입니다.")
        private LocalDate contractStartDate;

        private LocalDate contractEndDate;

        @NotNull(message = "급여 지급일은 필수입니다.")
        @Min(value = 1, message = "급여 지급일은 1일 이상이어야 합니다.")
        @Max(value = 31, message = "급여 지급일은 31일 이하여야 합니다.")
        private Integer paymentDay;

        @NotNull(message = "4대보험 적용 여부는 필수입니다.")
        private Boolean applyInsuranceAndTax;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContractUpdateRequest")
    public static class UpdateRequest {
        @DecimalMin(value = "10030.0", inclusive = true, message = "시급은 10,030 이상이어야 합니다.")
        private BigDecimal hourlyWage;

        @Size(min = 1, message = "최소 1개의 근무 요일을 선택해야 합니다.")
        private List<@Min(value = 1, message = "근무 요일은 1~7 사이여야 합니다.") @Max(value = 7, message = "근무 요일은 1~7 사이여야 합니다.") Integer> workDays;

        private LocalDate contractEndDate;

        @Min(value = 1, message = "급여 지급일은 1일 이상이어야 합니다.")
        @Max(value = 31, message = "급여 지급일은 31일 이하여야 합니다.")
        private Integer paymentDay;

        @NotNull(message = "4대보험 적용 여부는 필수입니다.")
        private Boolean applyInsuranceAndTax;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContractResponse")
    public static class Response {
        private Long id;
        private Long workplaceId;
        private String workplaceName;
        private Long workerId;
        private String workerName;
        private String workerCode;
        private String workerPhone;
        private BigDecimal hourlyWage;
        private String workDays;
        private LocalDate contractStartDate;
        private LocalDate contractEndDate;
        private Integer paymentDay;
        private Boolean isActive;
        private Boolean applyInsuranceAndTax;

        public static Response from(WorkerContract contract) {
            return Response.builder()
                    .id(contract.getId())
                    .workplaceId(contract.getWorkplace().getId())
                    .workplaceName(contract.getWorkplace().getName())
                    .workerId(contract.getWorker().getId())
                    .workerName(contract.getWorker().getUser().getName())
                    .workerCode(contract.getWorker().getWorkerCode())
                    .workerPhone(contract.getWorker().getUser().getPhone())
                    .hourlyWage(contract.getHourlyWage())
                    .workDays(contract.getWorkDays())
                    .contractStartDate(contract.getContractStartDate())
                    .contractEndDate(contract.getContractEndDate())
                    .paymentDay(contract.getPaymentDay())
                    .isActive(contract.getIsActive())
                    .applyInsuranceAndTax(contract.getApplyInsuranceAndTax())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContractListResponse")
    public static class ListResponse {
        private Long id;
        private String workerName;
        private String workerCode;
        private String workerPhone;
        private BigDecimal hourlyWage;
        private LocalDate contractStartDate;
        private LocalDate contractEndDate;
        private Boolean isActive;

        public static ListResponse from(WorkerContract contract) {
            return ListResponse.builder()
                    .id(contract.getId())
                    .workerName(contract.getWorker().getUser().getName())
                    .workerCode(contract.getWorker().getWorkerCode())
                    .workerPhone(contract.getWorker().getUser().getPhone())
                    .hourlyWage(contract.getHourlyWage())
                    .contractStartDate(contract.getContractStartDate())
                    .contractEndDate(contract.getContractEndDate())
                    .isActive(contract.getIsActive())
                    .build();
        }
    }
}
