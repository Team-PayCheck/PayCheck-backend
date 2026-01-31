package com.example.paycheck.domain.payment.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.payment.enums.PaymentMethod;
import com.example.paycheck.domain.payment.enums.PaymentStatus;
import com.example.paycheck.domain.salary.entity.Salary;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_id", nullable = false, unique = true)
    private Salary salary;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // 급여 송금이 완료된 시간 (status가 COMPLETED일 때만 값이 존재)
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    public void complete(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.paymentDate = LocalDateTime.now();
        this.transactionId = transactionId;
    }

    public void fail(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void resetToPending() {
        this.status = PaymentStatus.PENDING;
        this.paymentDate = null;
        this.transactionId = null;
        this.failureReason = null;
    }
}
