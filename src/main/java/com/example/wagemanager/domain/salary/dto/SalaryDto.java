package com.example.wagemanager.domain.salary.dto;

import com.example.wagemanager.domain.salary.entity.Salary;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class SalaryDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SalaryResponse")
    public static class Response {
        private Long id;
        private Long contractId;
        private Long workerId;
        private String workerName;
        private Long workplaceId;
        private String workplaceName;
        private Integer year;
        private Integer month;
        private BigDecimal totalWorkHours;
        private BigDecimal basePay;
        private BigDecimal overtimePay;
        private BigDecimal nightPay;
        private BigDecimal holidayPay;
        private BigDecimal totalGrossPay;
        private BigDecimal fourMajorInsurance;
        private BigDecimal incomeTax;
        private BigDecimal localIncomeTax;
        private BigDecimal totalDeduction;
        private BigDecimal netPay;
        private String paymentDueDate;

        public static Response from(Salary salary) {
            return Response.builder()
                    .id(salary.getId())
                    .contractId(salary.getContract().getId())
                    .workerId(salary.getContract().getWorker().getId())
                    .workerName(salary.getContract().getWorker().getUser().getName())
                    .workplaceId(salary.getContract().getWorkplace().getId())
                    .workplaceName(salary.getContract().getWorkplace().getName())
                    .year(salary.getYear())
                    .month(salary.getMonth())
                    .totalWorkHours(salary.getTotalWorkHours())
                    .basePay(salary.getBasePay())
                    .overtimePay(salary.getOvertimePay())
                    .nightPay(salary.getNightPay())
                    .holidayPay(salary.getHolidayPay())
                    .totalGrossPay(salary.getTotalGrossPay())
                    .fourMajorInsurance(salary.getFourMajorInsurance())
                    .incomeTax(salary.getIncomeTax())
                    .localIncomeTax(salary.getLocalIncomeTax())
                    .totalDeduction(salary.getTotalDeduction())
                    .netPay(salary.getNetPay())
                    .paymentDueDate(salary.getPaymentDueDate() != null ? salary.getPaymentDueDate().toString() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SalaryListResponse")
    public static class ListResponse {
        private Long id;
        private Long contractId;
        private String workerName;
        private Integer year;
        private Integer month;
        private BigDecimal totalGrossPay;
        private BigDecimal netPay;
        private String paymentDueDate;

        public static ListResponse from(Salary salary) {
            return ListResponse.builder()
                    .id(salary.getId())
                    .contractId(salary.getContract().getId())
                    .workerName(salary.getContract().getWorker().getUser().getName())
                    .year(salary.getYear())
                    .month(salary.getMonth())
                    .totalGrossPay(salary.getTotalGrossPay())
                    .netPay(salary.getNetPay())
                    .paymentDueDate(salary.getPaymentDueDate() != null ? salary.getPaymentDueDate().toString() : null)
                    .build();
        }
    }
}
