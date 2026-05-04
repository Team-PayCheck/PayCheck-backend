package com.example.paycheck.domain.workrecord.entity;

import com.example.paycheck.common.BaseEntity;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "work_record",
        indexes = {
                @Index(name = "idx_contract_date_status", columnList = "contract_id,work_date,status"),
                @Index(name = "idx_weekly_allowance_id", columnList = "weekly_allowance_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkRecord extends BaseEntity {

    // 근무 시간 및 급여 계산 상수
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(6, 0);
    private static final BigDecimal OVERTIME_RATE = BigDecimal.valueOf(1.5);
    private static final BigDecimal HOLIDAY_DAILY_THRESHOLD = BigDecimal.valueOf(8);
    private static final BigDecimal DAILY_THRESHOLD = BigDecimal.valueOf(8);

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

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

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

    @Column(name = "overtime_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal overtimeSalary = BigDecimal.ZERO;

    @Column(name = "total_salary", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSalary = BigDecimal.ZERO;

    // 근무 시간 수정 (근무 전/후 모두 사용)
    // 계산은 WorkRecordCalculationService에서 수행
    public void updateWorkTime(LocalTime startTime, LocalTime endTime, String memo) {
        this.startTime = startTime;
        this.endTime = endTime;
        if (memo != null) this.memo = memo;
        this.isModified = true;
    }

    // 근무 기록 수정
    // 계산은 WorkRecordCalculationService에서 수행
    public void updateWorkRecord(LocalTime startTime, LocalTime endTime, Integer breakMinutes, Integer totalWorkMinutes, String memo) {
        updateWorkRecord(null, startTime, endTime, breakMinutes, totalWorkMinutes, memo);
    }

    public void updateWorkRecord(LocalDate workDate, LocalTime startTime, LocalTime endTime, Integer breakMinutes, Integer totalWorkMinutes, String memo) {
        if (workDate != null) this.workDate = workDate;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (breakMinutes != null) this.breakMinutes = breakMinutes;
        if (totalWorkMinutes != null) this.totalWorkMinutes = totalWorkMinutes;
        if (memo != null) this.memo = memo;
        this.isModified = true;
    }

    // 근무 완료
    // 계산은 WorkRecordCalculationService에서 수행
    public void complete() {
        this.status = WorkRecordStatus.COMPLETED;
    }

    // 소프트 삭제
    public void markAsDeleted() {
        if (this.status == WorkRecordStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 근무 기록입니다.");
        }
        this.status = WorkRecordStatus.DELETED;
    }

    // 휴일 정보와 사업장 규모를 고려한 근무 시간 분류 계산
    // WorkRecordCalculationService에서 호출됨
    public void calculateHoursWithHolidayInfo(boolean isHoliday, boolean isSmallWorkplace) {
        // 전체 근무 시간 계산 (자정을 넘는 경우 처리)
        long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
        if (endTime.isBefore(startTime)) {
            minutes += 24 * 60; // 자정을 넘는 경우 24시간 추가
        }
        this.totalHours = BigDecimal.valueOf(minutes).subtract(BigDecimal.valueOf(this.breakMinutes))
                .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

        // 실제 근무 시간 계산 (전체 시간 - 휴식 시간)
        this.totalWorkMinutes = (int) (minutes - this.breakMinutes);

        // 연장 시간 계산 (8시간 초과분)
        if (this.totalHours.compareTo(DAILY_THRESHOLD) > 0) {
            this.overtimeHours = this.totalHours.subtract(DAILY_THRESHOLD);
        } else {
            this.overtimeHours = BigDecimal.ZERO;
        }

        // 야간 시간과 주간 시간 분류 (자정을 넘는 경우 처리)
        BigDecimal nightHours = BigDecimal.ZERO;

        LocalTime nightStart = NIGHT_SHIFT_START; // 22:00
        LocalTime nightEnd = NIGHT_SHIFT_END;     // 06:00

        boolean crossesMidnight = endTime.isBefore(startTime);

        if (crossesMidnight) {
            // 자정을 넘는 경우 (예: 22:00-06:00)
            if (!startTime.isBefore(nightStart)) {
                long nightMinutes1 = java.time.Duration.between(startTime, LocalTime.MAX).toMinutes() + 1;
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes1).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            } else {
                long nightMinutes1 = java.time.Duration.between(nightStart, LocalTime.MAX).toMinutes() + 1;
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes1).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            }

            if (endTime.isBefore(nightEnd) || endTime.equals(LocalTime.MIN)) {
                long nightMinutes2 = java.time.Duration.between(LocalTime.MIN, endTime).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes2).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            } else {
                long nightMinutes2 = java.time.Duration.between(LocalTime.MIN, nightEnd).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes2).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            }
        } else {
            if (startTime.isBefore(nightEnd)) {
                LocalTime actualEnd = endTime.isBefore(nightEnd) ? endTime : nightEnd;
                long nightMinutes = java.time.Duration.between(startTime, actualEnd).toMinutes();
                nightHours = BigDecimal.valueOf(nightMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
            }

            if (endTime.isAfter(nightStart)) {
                LocalTime actualStart = startTime.isAfter(nightStart) ? startTime : nightStart;
                long nightMinutes = java.time.Duration.between(actualStart, endTime).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            }
        }

        // 휴게시간 비율에 따른 야간 시간 차감 (간소화된 정책: 전체 시간 대비 야간 시간 비율로 차감)
        if (this.breakMinutes > 0 && this.totalHours.compareTo(BigDecimal.ZERO) > 0) {
             BigDecimal breakHours = BigDecimal.valueOf(this.breakMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
             BigDecimal nightRatio = nightHours.divide(this.totalHours.add(breakHours), 4, java.math.RoundingMode.HALF_UP);
             nightHours = nightHours.subtract(breakHours.multiply(nightRatio)).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        this.nightHours = nightHours.max(BigDecimal.ZERO);

        // 휴일 여부에 따라 분류
        if (isHoliday) {
            this.holidayHours = this.totalHours;
            this.regularHours = BigDecimal.ZERO;
        } else {
            this.regularHours = this.totalHours.subtract(this.nightHours).max(BigDecimal.ZERO);
            this.holidayHours = BigDecimal.ZERO;
        }
    }

    // 사업장 규모를 고려한 급여 계산
    // WorkRecordCalculationService에서 호출됨
    public void calculateSalaryWithAllowanceRules(boolean isSmallWorkplace) {
        BigDecimal hourlyWage = this.contract.getHourlyWage();
        BigDecimal premiumRate = BigDecimal.valueOf(0.5);

        // 1. 기본급 계산 (모든 사업장 공통: 1.0배)
        this.baseSalary = this.totalHours.multiply(hourlyWage);

        if (isSmallWorkplace) {
            // 5인 미만 사업장: 가산 수당 없음
            this.overtimeSalary = BigDecimal.ZERO;
            this.nightSalary = BigDecimal.ZERO;
            this.holidaySalary = BigDecimal.ZERO;
        } else {
            // 5인 이상 사업장: 가산 수당 (0.5배씩) 적용
            
            // 연장 가산 (8시간 초과분 0.5배)
            this.overtimeSalary = this.overtimeHours.multiply(hourlyWage).multiply(premiumRate);

            // 야간 가산 (22-06시 근무분 0.5배)
            this.nightSalary = this.nightHours.multiply(hourlyWage).multiply(premiumRate);

            // 휴일 가산 (휴일 근무분 0.5배)
            this.holidaySalary = this.holidayHours.multiply(hourlyWage).multiply(premiumRate);
        }

        // 총 급여 = 기본급(1.0) + 연장가산(0.5) + 야간가산(0.5) + 휴일가산(0.5)
        this.totalSalary = this.baseSalary
                .add(this.overtimeSalary)
                .add(this.nightSalary)
                .add(this.holidaySalary);
    }
}
