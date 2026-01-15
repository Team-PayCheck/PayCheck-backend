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
        this.totalHours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

        // 실제 근무 시간 계산 (전체 시간 - 휴식 시간)
        this.totalWorkMinutes = (int) (minutes - this.breakMinutes);

        // 야간 시간과 주간 시간 분류 (자정을 넘는 경우 처리)
        BigDecimal nightHours = BigDecimal.ZERO;
        BigDecimal dayHours = BigDecimal.ZERO;

        LocalTime nightStart = NIGHT_SHIFT_START; // 22:00
        LocalTime nightEnd = NIGHT_SHIFT_END;     // 06:00

        boolean crossesMidnight = endTime.isBefore(startTime);

        if (crossesMidnight) {
            // 자정을 넘는 경우 (예: 22:00-06:00)
            // 시작 시간이 22:00 이후이면 야간에 해당
            if (!startTime.isBefore(nightStart)) {
                // 22:00-24:00 구간의 야간 시간
                long nightMinutes1 = java.time.Duration.between(startTime, LocalTime.MAX).toMinutes() + 1; // +1 for 24:00
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes1).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            } else if (startTime.isBefore(nightEnd)) {
                // 시작이 00:00-06:00 사이인 경우는 없음 (crossesMidnight이므로)
            } else {
                // 시작이 06:00-22:00 사이: 22:00-24:00 전체가 야간
                long nightMinutes1 = java.time.Duration.between(nightStart, LocalTime.MAX).toMinutes() + 1;
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes1).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            }

            // 종료 시간이 06:00 이전이면 야간에 해당
            if (endTime.isBefore(nightEnd) || endTime.equals(LocalTime.MIN)) {
                // 00:00-종료 시간 구간의 야간 시간
                long nightMinutes2 = java.time.Duration.between(LocalTime.MIN, endTime).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes2).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            } else {
                // 종료가 06:00 이후: 00:00-06:00 전체가 야간
                long nightMinutes2 = java.time.Duration.between(LocalTime.MIN, nightEnd).toMinutes();
                nightHours = nightHours.add(BigDecimal.valueOf(nightMinutes2).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            }
        } else {
            // 자정을 넘지 않는 경우 (예: 09:00-18:00, 23:00-05:00은 불가능)
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
        }

        // 주간 시간 = 전체 시간 - 야간 시간
        dayHours = this.totalHours.subtract(nightHours);

        // 휴일 여부에 따라 분류
        if (isHoliday) {
            // 휴일 근무 시 사업장 규모와 관계없이 holidayHours에 기록
            // 급여 가산은 calculateSalaryWithAllowanceRules()에서 사업장 규모에 따라 처리
            this.holidayHours = this.totalHours;
            this.nightHours = nightHours; // 야간 시간대는 별도 표시
            this.regularHours = BigDecimal.ZERO;
        } else {
            // 평일
            this.nightHours = nightHours;
            this.regularHours = dayHours;
            this.holidayHours = BigDecimal.ZERO;
        }
    }

    // 사업장 규모를 고려한 급여 계산
    // WorkRecordCalculationService에서 호출됨
    public void calculateSalaryWithAllowanceRules(boolean isSmallWorkplace) {
        BigDecimal hourlyWage = this.contract.getHourlyWage();

        // 사업장 규모에 따라 급여 계산 분기
        if (isSmallWorkplace) {
            // ===== 5인 미만 사업장 =====
            // 휴일/평일, 야간/주간 구분 없이 모든 시간을 기본 시급으로만 계산
            this.baseSalary = this.totalHours.multiply(hourlyWage);
            this.nightSalary = BigDecimal.ZERO;
            this.holidaySalary = BigDecimal.ZERO;
        } else {
            // ===== 5인 이상 사업장 =====
            // 휴일 근무 여부에 따라 급여 계산 분기
            if (this.holidayHours.compareTo(BigDecimal.ZERO) > 0) {
                // ----- 휴일 근무인 경우 -----
                // 휴일 가산 적용
                // holidayHours는 전체 시간, nightHours는 야간 시간대
                // 주간 시간 = 전체 휴일 시간 - 야간 시간
                BigDecimal dayHolidayHours = this.holidayHours.subtract(this.nightHours);

                if (this.holidayHours.compareTo(HOLIDAY_DAILY_THRESHOLD) <= 0) {
                    // Case 1: 휴일 전체 8시간 이하
                    // 주간: 휴일 50% 가산 (1.5배)
                    this.holidaySalary = dayHolidayHours.multiply(hourlyWage).multiply(OVERTIME_RATE);
                    // 야간: 휴일 50% + 야간 50% = 100% 가산 (2.0배)
                    this.nightSalary = this.nightHours.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.0));
                } else {
                    // Case 2: 휴일 전체 8시간 초과
                    // 8시간 이내 부분과 초과 부분을 야간/주간으로 분배
                    if (this.nightHours.compareTo(BigDecimal.ZERO) == 0) {
                        // 야간 없음: 모두 주간
                        BigDecimal overtime = this.holidayHours.subtract(HOLIDAY_DAILY_THRESHOLD);

                        // 처음 8시간: 휴일 50% (1.5배)
                        // 초과 시간: 휴일 50% + 연장 50% = 100% 가산 (2.0배)
                        this.holidaySalary = HOLIDAY_DAILY_THRESHOLD.multiply(hourlyWage).multiply(OVERTIME_RATE)
                            .add(overtime.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.0)));
                        this.nightSalary = BigDecimal.ZERO;
                    } else if (dayHolidayHours.compareTo(HOLIDAY_DAILY_THRESHOLD) >= 0) {
                        // 주간이 8시간 이상: 주간 8시간까지는 1.5배, 나머지는 모두 초과
                        BigDecimal dayOvertime = dayHolidayHours.subtract(HOLIDAY_DAILY_THRESHOLD);

                        // 주간 처음 8시간: 휴일 50% (1.5배)
                        // 주간 초과: 휴일 50% + 연장 50% (2.0배)
                        this.holidaySalary = HOLIDAY_DAILY_THRESHOLD.multiply(hourlyWage).multiply(OVERTIME_RATE)
                            .add(dayOvertime.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.0)));
                        // 야간은 모두 초과: 휴일 50% + 연장 50% + 야간 50% = 150% 가산 (2.5배)
                        this.nightSalary = this.nightHours.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.5));
                    } else {
                        // 주간 < 8시간: 주간은 모두 8시간 이내, 야간 일부가 8시간 초과
                        BigDecimal nightWithin8 = HOLIDAY_DAILY_THRESHOLD.subtract(dayHolidayHours);
                        BigDecimal nightOvertime = this.nightHours.subtract(nightWithin8);

                        // 주간: 모두 8시간 이내 (휴일 50%, 1.5배)
                        this.holidaySalary = dayHolidayHours.multiply(hourlyWage).multiply(OVERTIME_RATE);
                        // 야간 8시간 이내: 휴일 50% + 야간 50% (2.0배)
                        // 야간 초과: 휴일 50% + 연장 50% + 야간 50% (2.5배)
                        this.nightSalary = nightWithin8.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.0))
                            .add(nightOvertime.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.5)));
                    }
                }
                this.baseSalary = BigDecimal.ZERO; // 휴일이므로 기본급 없음
            } else {
                // ----- 평일 근무인 경우 -----
                this.holidaySalary = BigDecimal.ZERO;

                // 평일 총 근무 시간 = 주간 + 야간
                BigDecimal totalWeekdayHours = this.regularHours.add(this.nightHours);

                if (totalWeekdayHours.compareTo(DAILY_THRESHOLD) <= 0) {
                    // Case 1: 8시간 이하 - 연장 없음
                    // 주간: 1.0배
                    this.baseSalary = this.regularHours.multiply(hourlyWage);
                    // 야간: 1.5배 (야간 가산만)
                    this.nightSalary = this.nightHours.multiply(hourlyWage).multiply(OVERTIME_RATE);
                } else {
                    // Case 2-4: 8시간 초과 - 연장수당 발생
                    if (this.nightHours.compareTo(BigDecimal.ZERO) == 0) {
                        // Case 2: 야간 없음 - 모두 주간
                        BigDecimal first8 = DAILY_THRESHOLD;
                        BigDecimal overtime = this.regularHours.subtract(first8);

                        // 처음 8시간: 1.0배
                        // 초과 시간: 1.5배 (연장 가산)
                        this.baseSalary = first8.multiply(hourlyWage)
                            .add(overtime.multiply(hourlyWage).multiply(OVERTIME_RATE));
                        this.nightSalary = BigDecimal.ZERO;
                    } else if (this.regularHours.compareTo(DAILY_THRESHOLD) >= 0) {
                        // Case 3: 주간이 8시간 이상
                        BigDecimal regularOvertime = this.regularHours.subtract(DAILY_THRESHOLD);

                        // 주간 처음 8시간: 1.0배
                        // 주간 초과: 1.5배 (연장 가산)
                        this.baseSalary = DAILY_THRESHOLD.multiply(hourlyWage)
                            .add(regularOvertime.multiply(hourlyWage).multiply(OVERTIME_RATE));
                        // 야간은 모두 초과: 1.5배(연장) + 0.5배(야간) = 2.0배
                        this.nightSalary = this.nightHours.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.0));
                    } else {
                        // Case 4: 주간 < 8시간 - 야간 일부가 8시간 초과
                        BigDecimal nightWithin8 = DAILY_THRESHOLD.subtract(this.regularHours);
                        BigDecimal nightOvertime = this.nightHours.subtract(nightWithin8);

                        // 주간: 모두 8시간 이내 (1.0배)
                        this.baseSalary = this.regularHours.multiply(hourlyWage);
                        // 야간 8시간 이내: 1.5배 (야간 가산)
                        // 야간 초과: 2.0배 (연장 + 야간 가산)
                        this.nightSalary = nightWithin8.multiply(hourlyWage).multiply(OVERTIME_RATE)
                            .add(nightOvertime.multiply(hourlyWage).multiply(BigDecimal.valueOf(2.0)));
                    }
                }
            }
        }

        // 총 급여 = 기본급 + 야간급 + 휴일급
        this.totalSalary = this.baseSalary
                .add(this.nightSalary)
                .add(this.holidaySalary);
    }
}
