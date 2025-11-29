package com.example.wagemanager.domain.contract.entity;

import com.example.wagemanager.common.BaseEntity;
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

    @Column(name = "work_days", nullable = false, columnDefinition = "JSON")
    private String workDays; // JSON: [1,2,3,4,5,6,7]

    @Column(name = "contract_start_date", nullable = false)
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "payment_day", nullable = false)
    private Integer paymentDay;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "apply_insurance_and_tax", nullable = false)
    @Builder.Default
    private Boolean applyInsuranceAndTax = false;

    public void update(BigDecimal hourlyWage, String workDays, LocalDate contractEndDate, Integer paymentDay,
                       Boolean applyInsuranceAndTax) {
        if (hourlyWage != null) this.hourlyWage = hourlyWage;
        if (workDays != null) this.workDays = workDays;
        if (contractEndDate != null) this.contractEndDate = contractEndDate;
        if (paymentDay != null) this.paymentDay = paymentDay;
        if (applyInsuranceAndTax != null) this.applyInsuranceAndTax = applyInsuranceAndTax;
    }

    public void terminate() {
        this.isActive = false;
        this.contractEndDate = LocalDate.now();
    }
}
