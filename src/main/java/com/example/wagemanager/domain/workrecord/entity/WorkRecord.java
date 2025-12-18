package com.example.wagemanager.domain.workrecord.entity;

import com.example.wagemanager.common.BaseEntity;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
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

    // 근무 시간 및 급여 계산 상수
    private static final int WEEKEND_DAY_THRESHOLD = 6;
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(6, 0);
    private static final BigDecimal OVERTIME_RATE = BigDecimal.valueOf(1.5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private WorkerContract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_allowance_id")
    private WeeklyAllowance weeklyAllowance;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // WeeklyAllowance 할당 (WeeklyAllowance 생성 시 사용)
    public void assignToWeeklyAllowance(WeeklyAllowance weeklyAllowance) {
        this.weeklyAllowance = weeklyAllowance;
    }

    // WeeklyAllowance의 리스트에 WorkRecord 추가 (양방향 관계 동기화)
    public void addToWeeklyAllowance() {
        if (this.weeklyAllowance != null && !this.weeklyAllowance.getWorkRecords().contains(this)) {
            this.weeklyAllowance.getWorkRecords().add(this);
        }
    }

    // WeeklyAllowance의 리스트에서 WorkRecord 제거 (양방향 관계 동기화)
    public void removeFromWeeklyAllowance() {
        if (this.weeklyAllowance != null) {
            this.weeklyAllowance.getWorkRecords().remove(this);
        }
    }

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

    // 급여 칼럼 (세금 공제 전 금액)
    @Column(name = "base_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(name = "night_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal nightSalary = BigDecimal.ZERO;

    @Column(name = "holiday_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal holidaySalary = BigDecimal.ZERO;

    @Column(name = "total_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalary = BigDecimal.ZERO;

    // JPA 생명주기 콜백: 엔티티 저장/수정 전 자동 계산
    @PrePersist
    @PreUpdate
    private void prePersist() {
        if (startTime != null && endTime != null) {
            calculateHours();
            if (status == WorkRecordStatus.COMPLETED) {
                calculateTotalSalary();
            }
        }
    }

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

    // 소프트 삭제
    public void markAsDeleted() {
        if (this.status == WorkRecordStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 근무 기록입니다.");
        }
        this.status = WorkRecordStatus.DELETED;
    }

    // 근무 시간 분류 계산
    // 전체 근무 시간을 일반 근무, 야간 근무, 휴일 근무 시간으로 분류
    private void calculateHours() {
        // 전체 근무 시간 계산
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        this.totalHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

        // 실제 근무 시간 계산 (전체 시간 - 휴식 시간)
        this.totalWorkMinutes = (int) (minutes - this.breakMinutes);

        // 휴일 여부 판별 (일요일=0, 토요일=6)
        boolean isHoliday = workDate.getDayOfWeek().getValue() >= WEEKEND_DAY_THRESHOLD;

        // 야간 시간과 주간 시간 분류
        BigDecimal nightHours = BigDecimal.ZERO;
        BigDecimal dayHours = BigDecimal.ZERO;

        LocalTime nightStart = NIGHT_SHIFT_START;
        LocalTime nightEnd = NIGHT_SHIFT_END;

        if (startTime.isBefore(nightEnd)) {
            // 06시 이전에 시작: 야간 근무
            LocalTime actualEnd = endTime.isBefore(nightEnd) ? endTime : nightEnd;
            long nightMinutes = java.time.Duration.between(startTime, actualEnd).toMinutes();
            nightHours = BigDecimal.valueOf(nightMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        }

        if (endTime.isAfter(nightStart)) {
            // 22시 이후에 종료: 야간 근무
            LocalTime actualStart = startTime.isAfter(nightStart) ? startTime : nightStart;
            long nightMinutes = java.time.Duration.between(actualStart, endTime).toMinutes();
            nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
        }

        // 주간 시간 = 전체 시간 - 야간 시간
        dayHours = this.totalHours.subtract(nightHours);

        // 휴일 여부에 따라 분류
        if (isHoliday) {
            this.holidayHours = dayHours;
            this.nightHours = nightHours;
            this.regularHours = BigDecimal.ZERO;
        } else {
            this.nightHours = nightHours;
            this.regularHours = dayHours;
            this.holidayHours = BigDecimal.ZERO;
        }
    }

    // 총 급여 계산 (세금 공제, 주휴수당, 연장근로 미적용)
    // 각 근무 시간(일반/야간/휴일)에 맞는 시급을 적용하여 그 날의 총 급여 계산
    private void calculateTotalSalary() {
        BigDecimal hourlyWage = this.contract.getHourlyWage();

        // 기본 급여 = 일반 근무 시간 × 기본시급
        this.baseSalary = this.regularHours.multiply(hourlyWage);

        // 야간 급여 = 야간 근무 시간 × (기본시급 × 1.5)
        BigDecimal nightWage = hourlyWage.multiply(OVERTIME_RATE);
        this.nightSalary = this.nightHours.multiply(nightWage);

        // 휴일 급여 = 휴일 근무 시간 × (기본시급 × 1.5)
        BigDecimal holidayWage = hourlyWage.multiply(OVERTIME_RATE);
        this.holidaySalary = this.holidayHours.multiply(holidayWage);

        // 총 급여 = 기본급 + 야간급 + 휴일급
        this.totalSalary = this.baseSalary.add(this.nightSalary).add(this.holidaySalary);
    }
}
