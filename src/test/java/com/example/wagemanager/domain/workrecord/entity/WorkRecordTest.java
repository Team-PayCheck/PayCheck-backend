package com.example.wagemanager.domain.workrecord.entity;

import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WorkRecord 엔티티 테스트")
class WorkRecordTest {

    private WorkRecord workRecord;
    private WorkerContract mockContract;

    @BeforeEach
    void setUp() {
        mockContract = mock(WorkerContract.class);
        when(mockContract.getHourlyWage()).thenReturn(BigDecimal.valueOf(10000));

        workRecord = WorkRecord.builder()
                .id(1L)
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .status(WorkRecordStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("WeeklyAllowance 할당")
    void assignToWeeklyAllowance() {
        // given
        WeeklyAllowance weeklyAllowance = mock(WeeklyAllowance.class);

        // when
        workRecord.assignToWeeklyAllowance(weeklyAllowance);

        // then
        assertThat(workRecord.getWeeklyAllowance()).isEqualTo(weeklyAllowance);
    }

    @Test
    @DisplayName("근무 시간 수정 - SCHEDULED 상태")
    void updateWorkTime_Scheduled() {
        // given
        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(19, 0);
        String memo = "시간 변경";

        // when
        workRecord.updateWorkTime(newStart, newEnd, memo);

        // then
        assertThat(workRecord.getStartTime()).isEqualTo(newStart);
        assertThat(workRecord.getEndTime()).isEqualTo(newEnd);
        assertThat(workRecord.getMemo()).isEqualTo(memo);
        assertThat(workRecord.getIsModified()).isTrue();
    }

    @Test
    @DisplayName("근무 시간 수정 - COMPLETED 상태 (재계산 발생)")
    void updateWorkTime_Completed() {
        // given
        workRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkRecordStatus.COMPLETED)
                .build();

        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(19, 0);

        // when
        workRecord.updateWorkTime(newStart, newEnd, "시간 변경");

        // then
        assertThat(workRecord.getStartTime()).isEqualTo(newStart);
        assertThat(workRecord.getEndTime()).isEqualTo(newEnd);
        assertThat(workRecord.getIsModified()).isTrue();
    }

    @Test
    @DisplayName("근무 기록 수정")
    void updateWorkRecord() {
        // given
        LocalTime newStart = LocalTime.of(8, 0);
        LocalTime newEnd = LocalTime.of(17, 0);
        Integer newBreakMinutes = 30;
        Integer newTotalWorkMinutes = 510;
        String memo = "근무 기록 수정";

        // when
        workRecord.updateWorkRecord(newStart, newEnd, newBreakMinutes, newTotalWorkMinutes, memo);

        // then
        assertThat(workRecord.getStartTime()).isEqualTo(newStart);
        assertThat(workRecord.getEndTime()).isEqualTo(newEnd);
        assertThat(workRecord.getBreakMinutes()).isEqualTo(newBreakMinutes);
        assertThat(workRecord.getTotalWorkMinutes()).isEqualTo(newTotalWorkMinutes);
        assertThat(workRecord.getMemo()).isEqualTo(memo);
        assertThat(workRecord.getIsModified()).isTrue();
    }

    @Test
    @DisplayName("근무 완료 처리")
    void complete() {
        // when
        workRecord.complete();

        // then
        assertThat(workRecord.getStatus()).isEqualTo(WorkRecordStatus.COMPLETED);
    }

    @Test
    @DisplayName("소프트 삭제")
    void markAsDeleted() {
        // when
        workRecord.markAsDeleted();

        // then
        assertThat(workRecord.getStatus()).isEqualTo(WorkRecordStatus.DELETED);
    }

    @Test
    @DisplayName("소프트 삭제 실패 - 이미 삭제된 기록")
    void markAsDeleted_AlreadyDeleted() {
        // given
        workRecord.markAsDeleted();

        // when & then
        assertThatThrownBy(() -> workRecord.markAsDeleted())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 삭제된 근무 기록입니다");
    }

    @Test
    @DisplayName("5인 미만 - 평일 야간 근무: 기본시급만 지급")
    void calculateSalary_SmallWorkplace_WeekdayNight() {
        // given: Monday 22:00-06:00 (8 hours night)
        WorkRecord nightWorkRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        nightWorkRecord.calculateHoursWithHolidayInfo(false, true); // not holiday, small workplace
        nightWorkRecord.calculateSalaryWithAllowanceRules(true); // small workplace

        // then
        assertThat(nightWorkRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(nightWorkRecord.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000)); // 8 × 10000 × 1.0
        assertThat(nightWorkRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nightWorkRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nightWorkRecord.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
    }

    @Test
    @DisplayName("5인 미만 - 휴일 야간 근무: 기본시급만 지급 (휴일 가산도 미적용)")
    void calculateSalary_SmallWorkplace_HolidayNight() {
        // given: Sunday 22:00-06:00 (8 hours night on holiday)
        WorkRecord nightWorkRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        nightWorkRecord.calculateHoursWithHolidayInfo(true, true); // holiday, small workplace
        nightWorkRecord.calculateSalaryWithAllowanceRules(true); // small workplace

        // then
        assertThat(nightWorkRecord.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0)); // 휴일 시간 기록됨
        assertThat(nightWorkRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(nightWorkRecord.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000)); // 8 × 10000 × 1.0
        assertThat(nightWorkRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nightWorkRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO); // 휴일 가산 없음
        assertThat(nightWorkRecord.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
    }

    @Test
    @DisplayName("5인 미만 - 휴일 혼합 근무 (주간+야간): 모두 기본시급")
    void calculateSalary_SmallWorkplace_HolidayMixed() {
        // given: Sunday 20:00-06:00 (2 day hours + 8 night hours)
        WorkRecord mixedWorkRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        mixedWorkRecord.calculateHoursWithHolidayInfo(true, true); // holiday, small workplace
        mixedWorkRecord.calculateSalaryWithAllowanceRules(true); // small workplace

        // then
        assertThat(mixedWorkRecord.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0)); // 전체 휴일 시간
        assertThat(mixedWorkRecord.getRegularHours()).isEqualByComparingTo(BigDecimal.ZERO); // 휴일이므로 0
        assertThat(mixedWorkRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0)); // 22:00-06:00
        assertThat(mixedWorkRecord.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(100000)); // 전체 10시간 × 10000
        assertThat(mixedWorkRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mixedWorkRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO); // 휴일 가산 없음
        assertThat(mixedWorkRecord.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    @Test
    @DisplayName("5인 이상 - 평일 야간 근무: 1.5배 야간수당 지급")
    void calculateSalary_LargeWorkplace_WeekdayNight_RegressionTest() {
        // given: Monday 22:00-06:00 (8 hours night)
        WorkRecord nightWorkRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        nightWorkRecord.calculateHoursWithHolidayInfo(false, false); // not holiday, large workplace
        nightWorkRecord.calculateSalaryWithAllowanceRules(false); // large workplace

        // then
        assertThat(nightWorkRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(nightWorkRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(120000)); // 8 × 10000 × 1.5
        assertThat(nightWorkRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nightWorkRecord.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(120000));
    }

    @Test
    @DisplayName("5인 이상 - 휴일 야간 8시간 이하: 2.0배 지급")
    void calculateSalary_LargeWorkplace_HolidayNight_Under8Hours_RegressionTest() {
        // given: Sunday 22:00-06:00 (8 hours night on holiday)
        WorkRecord nightWorkRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        nightWorkRecord.calculateHoursWithHolidayInfo(true, false); // holiday, large workplace
        nightWorkRecord.calculateSalaryWithAllowanceRules(false); // large workplace

        // then
        assertThat(nightWorkRecord.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(nightWorkRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(nightWorkRecord.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO); // 휴일이므로 0
        assertThat(nightWorkRecord.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(160000)); // 8 × 10000 × 2.0
        assertThat(nightWorkRecord.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO); // all night hours
        assertThat(nightWorkRecord.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(160000));
    }

    @Test
    @DisplayName("엣지 케이스 - 23:00-00:00 (자정 정각 종료, 1시간 야간 근무)")
    void calculateHours_EdgeCase_23To00() {
        // given: 23:00-00:00 (1 hour night work ending exactly at midnight)
        WorkRecord edgeCaseRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(0, 0))
                .breakMinutes(0)
                .build();

        // when
        edgeCaseRecord.calculateHoursWithHolidayInfo(false, false);

        // then
        assertThat(edgeCaseRecord.getTotalHours()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        assertThat(edgeCaseRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        assertThat(edgeCaseRecord.getRegularHours()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("엣지 케이스 - 23:00-12:00 (자정 넘어서 낮까지, 13시간)")
    void calculateHours_EdgeCase_23To12() {
        // given: 23:00-12:00 next day (13 hours: 7 night + 6 day)
        WorkRecord longShiftRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(12, 0))
                .breakMinutes(0)
                .build();

        // when
        longShiftRecord.calculateHoursWithHolidayInfo(false, false);

        // then
        // Total: 23:00-00:00 (1h) + 00:00-06:00 (6h) + 06:00-12:00 (6h) = 13h
        // Night: 23:00-00:00 (1h) + 00:00-06:00 (6h) = 7h
        // Day: 06:00-12:00 = 6h
        assertThat(longShiftRecord.getTotalHours()).isEqualByComparingTo(BigDecimal.valueOf(13.0));
        assertThat(longShiftRecord.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        assertThat(longShiftRecord.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(6.0));
    }

    // ===== 평일 1일 8시간 초과 연장수당 테스트 =====

    @Test
    @DisplayName("평일 8시간 이하 근무 - 연장수당 없음")
    void calculateSalary_Weekday_Within8Hours() {
        // given: 09:00-17:00 (8시간, 야간 없음)
        WorkRecord weekdayWork = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .breakMinutes(0)
                .build();

        // when
        weekdayWork.calculateHoursWithHolidayInfo(false, false); // 평일, 5인 이상
        weekdayWork.calculateSalaryWithAllowanceRules(false);

        // then
        assertThat(weekdayWork.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(weekdayWork.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(weekdayWork.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000)); // 8 × 10000 × 1.0
        assertThat(weekdayWork.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(weekdayWork.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
    }

    @Test
    @DisplayName("평일 10시간 근무 (야간 없음) - 2시간 연장수당 발생")
    void calculateSalary_Weekday_10Hours_NoNight() {
        // given: 09:00-19:00 (10시간, 야간 없음)
        WorkRecord weekdayWork = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(0)
                .build();

        // when
        weekdayWork.calculateHoursWithHolidayInfo(false, false); // 평일, 5인 이상
        weekdayWork.calculateSalaryWithAllowanceRules(false);

        // then
        assertThat(weekdayWork.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(weekdayWork.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        // baseSalary = 8 × 10000 × 1.0 + 2 × 10000 × 1.5 = 80000 + 30000 = 110000
        assertThat(weekdayWork.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(110000));
        assertThat(weekdayWork.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(weekdayWork.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(110000));
    }

    @Test
    @DisplayName("평일 10시간 근무 (주간 8 + 야간 2) - 연장+야간 중복")
    void calculateSalary_Weekday_10Hours_WithNight() {
        // given: 14:00-24:00 (10시간: 주간 8시간 + 야간 2시간)
        WorkRecord weekdayWork = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(0, 0)) // 자정
                .breakMinutes(0)
                .build();

        // when
        weekdayWork.calculateHoursWithHolidayInfo(false, false); // 평일, 5인 이상
        weekdayWork.calculateSalaryWithAllowanceRules(false);

        // then
        // 14:00-22:00 = 8시간 (주간), 22:00-24:00 = 2시간 (야간)
        assertThat(weekdayWork.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(weekdayWork.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        // baseSalary = 8 × 10000 × 1.0 = 80000 (주간 8시간까지는 기본)
        assertThat(weekdayWork.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        // nightSalary = 2 × 10000 × 2.0 = 40000 (연장 + 야간)
        assertThat(weekdayWork.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(weekdayWork.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(120000));
    }

    @Test
    @DisplayName("평일 10시간 근무 (주간 2 + 야간 8) - 야간 일부 연장")
    void calculateSalary_Weekday_10Hours_MostlyNight() {
        // given: 20:00-06:00 (10시간: 주간 2시간 + 야간 8시간)
        WorkRecord weekdayWork = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        weekdayWork.calculateHoursWithHolidayInfo(false, false); // 평일, 5인 이상
        weekdayWork.calculateSalaryWithAllowanceRules(false);

        // then
        // 20:00-22:00 = 2시간 (주간), 22:00-06:00 = 8시간 (야간)
        assertThat(weekdayWork.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        assertThat(weekdayWork.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        // baseSalary = 2 × 10000 × 1.0 = 20000 (주간 전체가 8시간 이내)
        assertThat(weekdayWork.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        // nightSalary = 6 × 10000 × 1.5 + 2 × 10000 × 2.0 = 90000 + 40000 = 130000
        // (8시간까지 6시간: 야간만, 초과 2시간: 연장+야간)
        assertThat(weekdayWork.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(130000));
        assertThat(weekdayWork.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }

    @Test
    @DisplayName("평일 13시간 근무 (자정 넘김) - 복잡한 연장 계산")
    void calculateSalary_Weekday_13Hours_OverMidnight() {
        // given: 23:00-12:00 (13시간: 야간 7시간 + 주간 6시간)
        WorkRecord weekdayWork = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(12, 0))
                .breakMinutes(0)
                .build();

        // when
        weekdayWork.calculateHoursWithHolidayInfo(false, false); // 평일, 5인 이상
        weekdayWork.calculateSalaryWithAllowanceRules(false);

        // then
        // 23:00-06:00 = 7시간 (야간), 06:00-12:00 = 6시간 (주간)
        assertThat(weekdayWork.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(6.0));
        assertThat(weekdayWork.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(7.0));
        // baseSalary = 6 × 10000 × 1.0 = 60000 (주간 전체가 8시간 이내)
        assertThat(weekdayWork.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        // nightSalary = 2 × 10000 × 1.5 + 5 × 10000 × 2.0 = 30000 + 100000 = 130000
        // (8시간까지 2시간: 야간만, 초과 5시간: 연장+야간)
        assertThat(weekdayWork.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(130000));
        assertThat(weekdayWork.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(190000));
    }

    @Test
    @DisplayName("5인 미만 사업장 - 평일 10시간 근무도 가산 없음")
    void calculateSalary_SmallWorkplace_Weekday_10Hours() {
        // given: 09:00-19:00 (10시간, 야간 없음)
        WorkRecord weekdayWork = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(0)
                .build();

        // when
        weekdayWork.calculateHoursWithHolidayInfo(false, true); // 평일, 5인 미만
        weekdayWork.calculateSalaryWithAllowanceRules(true); // 5인 미만

        // then
        assertThat(weekdayWork.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(weekdayWork.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        // baseSalary = 10 × 10000 × 1.0 = 100000 (연장 가산 없음)
        assertThat(weekdayWork.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(weekdayWork.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(weekdayWork.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    // ===== 5인 이상 사업장 체계적 테스트 =====

    // ----- (1) 평일 근무 케이스 -----

    @Test
    @DisplayName("평일 1-1: 총 8시간 이하, 야간 없음 (09:00-17:00)")
    void calculateSalary_Weekday_Under8_NoNight() {
        // given: 09:00-17:00 (8시간, 야간 없음)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(false, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        assertThat(work.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 8시간: 1.0배
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
    }

    @Test
    @DisplayName("평일 1-2: 총 8시간 이하, 야간 있음 (02:00-10:00, 주간 4시간 + 야간 4시간)")
    void calculateSalary_Weekday_Under8_WithNight() {
        // given: 02:00-10:00 (8시간: 야간 4시간 + 주간 4시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(2, 0))
                .endTime(LocalTime.of(10, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(false, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 02:00-06:00 = 4시간 (야간), 06:00-10:00 = 4시간 (주간)
        assertThat(work.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        // 주간 4시간: 1.0배 = 40000
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        // 야간 4시간: 1.5배 = 60000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    @Test
    @DisplayName("평일 2-1: 총 8시간 초과, 주간 8시간 이상, 야간 없음 (09:00-19:00)")
    void calculateSalary_Weekday_Over8_RegularOver8_NoNight() {
        // given: 09:00-19:00 (10시간, 야간 없음)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(false, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        assertThat(work.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 8시간: 1.0배 = 80000, 주간 초과 2시간: 1.5배 = 30000
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(110000));
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(110000));
    }

    @Test
    @DisplayName("평일 2-2: 총 8시간 초과, 주간 8시간 이상, 야간 있음 (12:00-00:00, 주간 10시간 + 야간 2시간)")
    void calculateSalary_Weekday_Over8_RegularOver8_WithNight() {
        // given: 12:00-00:00 (12시간: 주간 10시간 + 야간 2시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(0, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(false, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 12:00-22:00 = 10시간 (주간), 22:00-00:00 = 2시간 (야간)
        assertThat(work.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        // 주간 8시간: 1.0배 = 80000, 주간 초과 2시간: 1.5배 = 30000
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(110000));
        // 야간 2시간 (모두 초과): 2.0배 (연장 + 야간) = 40000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }

    @Test
    @DisplayName("평일 2-3: 총 8시간 초과, 주간 8시간 미만, 야간 있음 (20:00-06:00, 주간 2시간 + 야간 8시간)")
    void calculateSalary_Weekday_Over8_RegularUnder8_WithNight() {
        // given: 20:00-06:00 (10시간: 주간 2시간 + 야간 8시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(20, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(false, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 20:00-22:00 = 2시간 (주간), 22:00-06:00 = 8시간 (야간)
        assertThat(work.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        // 주간 2시간 (8시간 이내): 1.0배 = 20000
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        // 야간 8시간 중 6시간(8시간 이내): 1.5배 = 90000, 2시간(초과): 2.0배 = 40000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(130000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }

    @Test
    @DisplayName("평일 2-4: 총 8시간 초과, 주간 없음, 야간 8시간 이상 (22:00-08:00, 야간 8시간 + 주간 2시간)")
    void calculateSalary_Weekday_Over8_NoRegular_NightOver8() {
        // given: 22:00-08:00 (10시간: 야간 8시간 + 주간 2시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15)) // Monday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(8, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(false, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 22:00-06:00 = 8시간 (야간), 06:00-08:00 = 2시간 (주간)
        assertThat(work.getRegularHours()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        // 주간 2시간 (8시간 이내): 1.0배 = 20000
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        // 야간 8시간 중 6시간(8시간 이내): 1.5배 = 90000, 2시간(초과): 2.0배 = 40000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(130000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }

    // ----- (2) 휴일 근무 케이스 -----

    @Test
    @DisplayName("휴일 1-1: 총 8시간 이하, 야간 없음 (10:00-18:00)")
    void calculateSalary_Holiday_Under8_NoNight() {
        // given: Sunday 10:00-18:00 (8시간, 야간 없음)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(true, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        assertThat(work.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        // 휴일 주간 8시간: 1.5배 = 120000
        assertThat(work.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(120000));
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(120000));
    }

    @Test
    @DisplayName("휴일 1-2: 총 8시간 이하, 야간 있음 (02:00-10:00, 야간 4시간 + 주간 4시간)")
    void calculateSalary_Holiday_Under8_WithNight() {
        // given: Sunday 02:00-10:00 (8시간: 야간 4시간 + 주간 4시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(2, 0))
                .endTime(LocalTime.of(10, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(true, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 전체 8시간 휴일, 그 중 야간 4시간
        assertThat(work.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 4시간 (휴일): 1.5배 = 60000
        assertThat(work.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        // 야간 4시간 (휴일 + 야간, 8시간 이내): 2.0배 = 80000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(140000));
    }

    @Test
    @DisplayName("휴일 2-1: 총 8시간 초과, 주간 8시간 이상, 야간 없음 (09:00-19:00)")
    void calculateSalary_Holiday_Over8_DayOver8_NoNight() {
        // given: Sunday 09:00-19:00 (10시간, 야간 없음)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(19, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(true, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        assertThat(work.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 8시간: 1.5배 = 120000, 주간 초과 2시간: 2.0배 = 40000
        assertThat(work.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(160000));
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(160000));
    }

    @Test
    @DisplayName("휴일 2-2: 총 8시간 초과, 주간 8시간 이상, 야간 있음 (12:00-00:00, 주간 10시간 + 야간 2시간)")
    void calculateSalary_Holiday_Over8_DayOver8_WithNight() {
        // given: Sunday 12:00-00:00 (12시간: 주간 10시간 + 야간 2시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(0, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(true, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 전체 12시간 휴일, 그 중 야간 2시간
        assertThat(work.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 10시간: 8시간 1.5배 + 2시간 2.0배 = 120000 + 40000 = 160000
        assertThat(work.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(160000));
        // 야간 2시간 (모두 초과, 휴일+연장+야간): 2.5배 = 50000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(210000));
    }

    @Test
    @DisplayName("휴일 2-3: 총 8시간 초과, 주간 8시간 미만, 야간 있음 (18:00-04:00, 주간 4시간 + 야간 6시간)")
    void calculateSalary_Holiday_Over8_DayUnder8_WithNight() {
        // given: Sunday 18:00-04:00 (10시간: 주간 4시간 + 야간 6시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(4, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(true, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 전체 10시간 휴일, 그 중 야간 6시간
        // 18:00-22:00 = 4시간 (주간), 22:00-04:00 = 6시간 (야간)
        assertThat(work.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(6.0));
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 4시간 (8시간 이내): 1.5배 = 60000
        assertThat(work.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        // 야간 6시간 중 4시간(8시간 이내): 2.0배 = 80000, 2시간(초과): 2.5배 = 50000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(130000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(190000));
    }

    @Test
    @DisplayName("휴일 2-4: 총 8시간 초과, 주간 없음, 야간 8시간 이상 (22:00-08:00, 야간 8시간 + 주간 2시간)")
    void calculateSalary_Holiday_Over8_NoDay_NightOver8() {
        // given: Sunday 22:00-08:00 (10시간: 야간 8시간 + 주간 2시간)
        WorkRecord work = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(8, 0))
                .breakMinutes(0)
                .build();

        // when
        work.calculateHoursWithHolidayInfo(true, false);
        work.calculateSalaryWithAllowanceRules(false);

        // then
        // 전체 10시간 휴일, 그 중 야간 8시간
        // 22:00-06:00 = 8시간 (야간), 06:00-08:00 = 2시간 (주간)
        assertThat(work.getHolidayHours()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(work.getNightHours()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(work.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        // 주간 2시간 (8시간 이내): 1.5배 = 30000
        assertThat(work.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        // 야간 8시간 중 6시간(8시간 이내): 2.0배 = 120000, 2시간(초과): 2.5배 = 50000
        assertThat(work.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(170000));
        assertThat(work.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(200000));
    }
}
