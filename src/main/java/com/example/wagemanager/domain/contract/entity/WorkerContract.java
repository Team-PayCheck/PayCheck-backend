package com.example.wagemanager.domain.contract.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.salary.util.DeductionCalculator;
import com.example.wagemanager.domain.workplace.entity.Workplace;
import com.example.wagemanager.domain.worker.entity.Worker;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "worker_contract")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkerContract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workplace_id", nullable = false)
    private Workplace workplace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "hourly_wage", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyWage;

    @Column(name = "work_schedules", nullable = false, columnDefinition = "JSON")
    private String workSchedules; // JSON: [{"dayOfWeek": 1, "startTime": "09:00", "endTime": "18:00"}, ...]

    @Column(name = "contract_start_date", nullable = false)
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "payment_day", nullable = false)
    private Integer paymentDay;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "payroll_deduction_type", nullable = false)
    @Builder.Default
    private DeductionCalculator.PayrollDeductionType payrollDeductionType = DeductionCalculator.PayrollDeductionType.PART_TIME_NONE;

    public void update(BigDecimal hourlyWage, String workSchedules, LocalDate contractEndDate, Integer paymentDay,
                       DeductionCalculator.PayrollDeductionType payrollDeductionType) {
        if (hourlyWage != null) this.hourlyWage = hourlyWage;
        if (workSchedules != null) this.workSchedules = workSchedules;
        if (contractEndDate != null) this.contractEndDate = contractEndDate;
        if (paymentDay != null) {
            if (paymentDay < 1 || paymentDay > 31) {
                throw new IllegalArgumentException("paymentDay must be between 1 and 31");
            }
            this.paymentDay = paymentDay;
        }
        if (payrollDeductionType != null) this.payrollDeductionType = payrollDeductionType;
    }

    public void terminate() {
        this.isActive = false;
        this.contractEndDate = LocalDate.now();
    }
}
