package com.example.wagemanager.domain.workrecord.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "work_record",
        indexes = {
                @Index(name = "idx_contract_date_status", columnList = "contract_id,work_date,status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private WorkerContract contract;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "break_minutes")
    @Builder.Default
    private Integer breakMinutes = 0;

    @Column(name = "total_work_minutes")
    @Builder.Default
    private Integer totalWorkMinutes = 0;

    @Column(name = "total_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalHours = BigDecimal.ZERO;

    @Column(name = "regular_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal regularHours = BigDecimal.ZERO;

    @Column(name = "night_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal nightHours = BigDecimal.ZERO;

    @Column(name = "holiday_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal holidayHours = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WorkRecordStatus status = WorkRecordStatus.SCHEDULED;

    @Column(name = "is_modified", nullable = false)
    @Builder.Default
    private Boolean isModified = false;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "total_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalary = null;

    // 근무 시간 수정 (근무 전/후 모두 사용)
    public void updateWorkTime(LocalTime startTime, LocalTime endTime, String memo) {
        this.startTime = startTime;
        this.endTime = endTime;
        if (memo != null) this.memo = memo;
        this.isModified = true;

        // 근무 완료 후 수정이면 시간 및 급여 재계산
        if (this.status == WorkRecordStatus.COMPLETED) {
            calculateHours();
            calculateTotalSalary();
        }
    }

    // 근무 기록 수정
    public void updateWorkRecord(LocalTime startTime, LocalTime endTime, Integer breakMinutes, Integer totalWorkMinutes, String memo) {
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (breakMinutes != null) this.breakMinutes = breakMinutes;
        if (totalWorkMinutes != null) this.totalWorkMinutes = totalWorkMinutes;
        if (memo != null) this.memo = memo;
        this.isModified = true;
        calculateHours();
        calculateTotalSalary();
    }

    // 근무 완료
    public void complete() {
        this.status = WorkRecordStatus.COMPLETED;
        calculateHours();
        calculateTotalSalary();
    }

    // 근무 시간 분류 계산
    // 전체 근무 시간을 일반 근무, 야간 근무, 휴일 근무 시간으로 분류
    private void calculateHours() {
        // 전체 근무 시간 계산
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        this.totalHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

        // TODO: 야간 근무 시간 분류 (22시~06시)
        // TODO: 휴일 근무 여부 판별 및 시간 분류
        // TODO: 위 두 조건을 제외한 시간을 일반 근무 시간으로 분류

        // 임시 로직 (실제 구현 필요)
        // 일반 근무 시간 (8시간 이내)
        if (this.totalHours.compareTo(BigDecimal.valueOf(8)) <= 0) {
            this.regularHours = this.totalHours;
        } else {
            this.regularHours = BigDecimal.valueOf(8);
        }

        // 야간 근무 (22시~06시) - 추후 구현
        this.nightHours = BigDecimal.ZERO;

        // 휴일 근무 - 추후 구현
        this.holidayHours = BigDecimal.ZERO;
    }

    // 총 급여 계산 (세금 공제, 주휴수당, 연장근로 미적용)
    // 각 근무 시간(일반/야간/휴일)에 맞는 시급을 적용하여 그 날의 총 급여 계산
    private void calculateTotalSalary() {
        // TODO: 계약 정보에서 기본 시급 조회
        // TODO: 야간 근무 시급 계산 (기본시급 + 야간 수당)
        // TODO: 휴일 근무 시급 계산 (기본시급 + 휴일 수당)
        // 총 급여 = (일반 시간 × 기본시급) + (야간 시간 × 야간시급) + (휴일 시간 × 휴일시급)

        // 임시 계산 (실제 구현 필요)
        this.totalSalary = BigDecimal.ZERO;
    }
}
