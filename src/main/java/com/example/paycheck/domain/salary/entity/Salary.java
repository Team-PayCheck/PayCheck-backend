package com.example.paycheck.domain.salary.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_salary_contract_year_month",
                columnNames = {"contract_id", "salary_year", "salary_month"}
        ),
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

    @Version
    private Long version;

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

    @Column(name = "weekly_paid_leave_amount", precision = 12, scale = 2)
    private BigDecimal weeklyPaidLeaveAmount;

    @Column(name = "total_gross_pay", precision = 12, scale = 2)
    private BigDecimal totalGrossPay;

    @Column(name = "four_major_insurance", precision = 12, scale = 2)
    private BigDecimal fourMajorInsurance;

    @Column(name = "national_pension", precision = 12, scale = 2)
    private BigDecimal nationalPension;

    @Column(name = "health_insurance", precision = 12, scale = 2)
    private BigDecimal healthInsurance;

    @Column(name = "long_term_care", precision = 12, scale = 2)
    private BigDecimal longTermCare;

    @Column(name = "employment_insurance", precision = 12, scale = 2)
    private BigDecimal employmentInsurance;

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

    @OneToOne(mappedBy = "salary", fetch = FetchType.LAZY)
    private Payment payment;

    public void updateCalculatedFields(
            BigDecimal totalWorkHours,
            BigDecimal basePay,
            BigDecimal overtimePay,
            BigDecimal nightPay,
            BigDecimal holidayPay,
            BigDecimal weeklyPaidLeaveAmount,
            BigDecimal totalGrossPay,
            BigDecimal fourMajorInsurance,
            BigDecimal nationalPension,
            BigDecimal healthInsurance,
            BigDecimal longTermCare,
            BigDecimal employmentInsurance,
            BigDecimal incomeTax,
            BigDecimal localIncomeTax,
            BigDecimal totalDeduction,
            BigDecimal netPay
    ) {
        this.totalWorkHours = totalWorkHours;
        this.basePay = basePay;
        this.overtimePay = overtimePay;
        this.nightPay = nightPay;
        this.holidayPay = holidayPay;
        this.weeklyPaidLeaveAmount = weeklyPaidLeaveAmount;
        this.totalGrossPay = totalGrossPay;
        this.fourMajorInsurance = fourMajorInsurance;
        this.nationalPension = nationalPension;
        this.healthInsurance = healthInsurance;
        this.longTermCare = longTermCare;
        this.employmentInsurance = employmentInsurance;
        this.incomeTax = incomeTax;
        this.localIncomeTax = localIncomeTax;
        this.totalDeduction = totalDeduction;
        this.netPay = netPay;
    }
}
