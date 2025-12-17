package com.example.wagemanager.domain.payment.entity;

import com.example.wagemanager.domain.payment.enums.PaymentMethod;
import com.example.wagemanager.domain.payment.enums.PaymentStatus;
import com.example.wagemanager.domain.salary.entity.Salary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Payment 엔티티 테스트")
class PaymentTest {

    private Payment payment;
    private Salary mockSalary;

    @BeforeEach
    void setUp() {
        mockSalary = mock(Salary.class);
        payment = Payment.builder()
                .id(1L)
                .salary(mockSalary)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("결제 완료 처리")
    void complete() {
        // given
        String transactionId = "TXN123456789";

        // when
        payment.complete(transactionId);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        assertThat(payment.getPaymentDate()).isNotNull();
    }

    @Test
    @DisplayName("결제 실패 처리")
    void fail() {
        // given
        String failureReason = "잔액 부족";

        // when
        payment.fail(failureReason);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo(failureReason);
    }

    @Test
    @DisplayName("결제 완료 후 실패 처리")
    void complete_Then_Fail() {
        // given
        payment.complete("TXN_FIRST");

        // when
        payment.fail("환불 요청");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("환불 요청");
        assertThat(payment.getTransactionId()).isEqualTo("TXN_FIRST");
    }
}
