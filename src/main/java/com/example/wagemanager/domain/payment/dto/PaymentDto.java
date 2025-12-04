package com.example.wagemanager.domain.payment.dto;

import com.example.wagemanager.domain.payment.entity.Payment;
import com.example.wagemanager.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class PaymentDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PaymentPaymentRequest")
    public static class PaymentRequest {
        @NotNull(message = "급여 ID는 필수입니다.")
        private Long salaryId;
        // 지급 방법은 항상 카카오페이로 고정
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PaymentResponse")
    public static class Response {
        private Long id;
        private Long salaryId;
        private Long workerId;
        private String workerName;
        private Long workplaceId;
        private String workplaceName;
        private Integer year;
        private Integer month;
        private BigDecimal netPay;
        private PaymentStatus status;
        private String paymentDate;
        private String transactionId;
        private String failureReason;
        private Boolean isPaid; // 송금 여부

        public static Response from(Payment payment) {
            return Response.builder()
                    .id(payment.getId())
                    .salaryId(payment.getSalary().getId())
                    .workerId(payment.getSalary().getContract().getWorker().getId())
                    .workerName(payment.getSalary().getContract().getWorker().getUser().getName())
                    .workplaceId(payment.getSalary().getContract().getWorkplace().getId())
                    .workplaceName(payment.getSalary().getContract().getWorkplace().getName())
                    .year(payment.getSalary().getYear())
                    .month(payment.getSalary().getMonth())
                    .netPay(payment.getSalary().getNetPay())
                    .status(payment.getStatus())
                    .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null)
                    .transactionId(payment.getTransactionId())
                    .failureReason(payment.getFailureReason())
                    .isPaid(payment.getStatus() == PaymentStatus.COMPLETED)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "PaymentListResponse")
    public static class ListResponse {
        private Long id;
        private Long salaryId;
        private String workerName;
        private Integer year;
        private Integer month;
        private BigDecimal netPay;
        private PaymentStatus status;
        private String paymentDate;
        private Boolean isPaid; // 송금 여부

        public static ListResponse from(Payment payment) {
            return ListResponse.builder()
                    .id(payment.getId())
                    .salaryId(payment.getSalary().getId())
                    .workerName(payment.getSalary().getContract().getWorker().getUser().getName())
                    .year(payment.getSalary().getYear())
                    .month(payment.getSalary().getMonth())
                    .netPay(payment.getSalary().getNetPay())
                    .status(payment.getStatus())
                    .paymentDate(payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null)
                    .isPaid(payment.getStatus() == PaymentStatus.COMPLETED)
                    .build();
        }
    }
}
