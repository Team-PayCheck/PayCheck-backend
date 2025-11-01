package com.example.wagemanager.domain.salary.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary",
        indexes = {
                @Index(name = "idx_contract_year_month", columnList = "contract_id,salary_year,salary_month")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Salary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private WorkerContract contract;

    @Column(name = "salary_year", nullable = false)
    private Integer year;

    @Column(name = "salary_month", nullable = false)
    private Integer month;

    @Column(name = "total_work_hours", precision = 10, scale = 2)
    private BigDecimal totalWorkHours;

    @Column(name = "base_pay", precision = 12, scale = 2)
    private BigDecimal basePay;

    @Column(name = "overtime_pay", precision = 12, scale = 2)
    private BigDecimal overtimePay;

    @Column(name = "night_pay", precision = 12, scale = 2)
    private BigDecimal nightPay;

    @Column(name = "holiday_pay", precision = 12, scale = 2)
    private BigDecimal holidayPay;

    @Column(name = "total_gross_pay", precision = 12, scale = 2)
    private BigDecimal totalGrossPay;

    @Column(name = "four_major_insurance", precision = 12, scale = 2)
    private BigDecimal fourMajorInsurance;

    @Column(name = "income_tax", precision = 12, scale = 2)
    private BigDecimal incomeTax;

    @Column(name = "local_income_tax", precision = 12, scale = 2)
    private BigDecimal localIncomeTax;

    @Column(name = "total_deduction", precision = 12, scale = 2)
    private BigDecimal totalDeduction;

    @Column(name = "net_pay", precision = 12, scale = 2)
    private BigDecimal netPay;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;
}
