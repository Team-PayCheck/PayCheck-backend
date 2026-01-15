package com.example.wagemanager.domain.allowance.entity;

import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WeeklyAllowance 엔티티 테스트")
class WeeklyAllowanceTest {

    private WeeklyAllowance weeklyAllowance;
    private WorkerContract mockContract;

    @BeforeEach
    void setUp() {
        mockContract = mock(WorkerContract.class);
        when(mockContract.getHourlyWage()).thenReturn(BigDecimal.valueOf(10000));

        weeklyAllowance = WeeklyAllowance.builder()
                .id(1L)
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .workRecords(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("주간 총 근무 시간 계산 - 근무 기록 없음")
    void calculateTotalWorkHours_NoWorkRecords() {
        // when
        weeklyAllowance.calculateTotalWorkHours();

        // then
        assertThat(weeklyAllowance.getTotalWorkHours()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("주간 총 근무 시간 계산 - DELETED 제외")
    void calculateTotalWorkHours_ExcludeDeleted() {
        // given
        WorkRecord completedRecord = mock(WorkRecord.class);
        when(completedRecord.getStatus()).thenReturn(WorkRecordStatus.COMPLETED);
        when(completedRecord.getTotalHours()).thenReturn(BigDecimal.valueOf(8));

        WorkRecord deletedRecord = mock(WorkRecord.class);
        when(deletedRecord.getStatus()).thenReturn(WorkRecordStatus.DELETED);
        when(deletedRecord.getTotalHours()).thenReturn(BigDecimal.valueOf(5));

        weeklyAllowance.getWorkRecords().addAll(Arrays.asList(completedRecord, deletedRecord));

        // when
        weeklyAllowance.calculateTotalWorkHours();

        // then
        assertThat(weeklyAllowance.getTotalWorkHours()).isEqualTo(BigDecimal.valueOf(8));
    }

    @Test
    @DisplayName("주간 총 근무 시간 계산 - 여러 근무 기록")
    void calculateTotalWorkHours_MultipleRecords() {
        // given
        WorkRecord record1 = mock(WorkRecord.class);
        when(record1.getStatus()).thenReturn(WorkRecordStatus.COMPLETED);
        when(record1.getTotalHours()).thenReturn(BigDecimal.valueOf(8));

        WorkRecord record2 = mock(WorkRecord.class);
        when(record2.getStatus()).thenReturn(WorkRecordStatus.COMPLETED); // COMPLETED만 수당 계산에 포함
        when(record2.getTotalHours()).thenReturn(BigDecimal.valueOf(7));

        weeklyAllowance.getWorkRecords().addAll(Arrays.asList(record1, record2));

        // when
        weeklyAllowance.calculateTotalWorkHours();

        // then
        assertThat(weeklyAllowance.getTotalWorkHours()).isEqualTo(BigDecimal.valueOf(15));
    }

    @Test
    @DisplayName("주휴수당 계산 - 주 15시간 미만 (미지급)")
    void calculateWeeklyPaidLeave_LessThan15Hours() {
        // given
        weeklyAllowance = WeeklyAllowance.builder()
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .totalWorkHours(BigDecimal.valueOf(14))
                .build();

        // when
        weeklyAllowance.calculateWeeklyPaidLeave();

        // then
        assertThat(weeklyAllowance.getWeeklyPaidLeaveAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("주휴수당 계산 - 주 15시간 이상 (지급)")
    void calculateWeeklyPaidLeave_MoreThan15Hours() {
        // given
        weeklyAllowance = WeeklyAllowance.builder()
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .totalWorkHours(BigDecimal.valueOf(20))
                .build();

        // when
        weeklyAllowance.calculateWeeklyPaidLeave();

        // then
        // (20 / 40) × 8 × 10000 = 40000
        assertThat(weeklyAllowance.getWeeklyPaidLeaveAmount()).isEqualTo(new BigDecimal("40000.00"));
    }

    @Test
    @DisplayName("주휴수당 계산 - 정확히 15시간")
    void calculateWeeklyPaidLeave_Exactly15Hours() {
        // given
        weeklyAllowance = WeeklyAllowance.builder()
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .totalWorkHours(BigDecimal.valueOf(15))
                .build();

        // when
        weeklyAllowance.calculateWeeklyPaidLeave();

        // then
        // (15 / 40) × 8 × 10000 = 30000
        assertThat(weeklyAllowance.getWeeklyPaidLeaveAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("연장수당 계산 - 주 40시간 이하 (미지급)")
    void calculateOvertime_LessThan40Hours() {
        // given
        weeklyAllowance = WeeklyAllowance.builder()
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .totalWorkHours(BigDecimal.valueOf(35))
                .build();

        // when
        weeklyAllowance.calculateOvertime(false); // large workplace

        // then
        assertThat(weeklyAllowance.getOvertimeHours()).isEqualTo(BigDecimal.ZERO);
        assertThat(weeklyAllowance.getOvertimeAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("연장수당 계산 - 주 40시간 초과 (지급)")
    void calculateOvertime_MoreThan40Hours() {
        // given
        weeklyAllowance = WeeklyAllowance.builder()
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .totalWorkHours(BigDecimal.valueOf(45))
                .build();

        // when
        weeklyAllowance.calculateOvertime(false); // large workplace

        // then
        // 초과 시간: 5시간
        assertThat(weeklyAllowance.getOvertimeHours()).isEqualTo(BigDecimal.valueOf(5));
        // 연장수당: 5 × 10000 × 1.5 = 75000
        assertThat(weeklyAllowance.getOvertimeAmount()).isEqualByComparingTo(new BigDecimal("75000"));
    }

    @Test
    @DisplayName("연장수당 계산 - 정확히 40시간")
    void calculateOvertime_Exactly40Hours() {
        // given
        weeklyAllowance = WeeklyAllowance.builder()
                .contract(mockContract)
                .weekStartDate(LocalDate.of(2024, 1, 1))
                .weekEndDate(LocalDate.of(2024, 1, 7))
                .totalWorkHours(BigDecimal.valueOf(40))
                .build();

        // when
        weeklyAllowance.calculateOvertime(false); // large workplace

        // then
        assertThat(weeklyAllowance.getOvertimeHours()).isEqualTo(BigDecimal.ZERO);
        assertThat(weeklyAllowance.getOvertimeAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("전체 계산 플로우 - 계산 순서 테스트")
    void calculateAll_FullFlow() {
        // given
        WorkRecord record1 = mock(WorkRecord.class);
        when(record1.getStatus()).thenReturn(WorkRecordStatus.COMPLETED);
        when(record1.getTotalHours()).thenReturn(BigDecimal.valueOf(25));

        WorkRecord record2 = mock(WorkRecord.class);
        when(record2.getStatus()).thenReturn(WorkRecordStatus.COMPLETED);
        when(record2.getTotalHours()).thenReturn(BigDecimal.valueOf(20));

        weeklyAllowance.getWorkRecords().addAll(Arrays.asList(record1, record2));

        // when
        weeklyAllowance.calculateTotalWorkHours();
        weeklyAllowance.calculateWeeklyPaidLeave();
        weeklyAllowance.calculateOvertime(false); // large workplace

        // then
        assertThat(weeklyAllowance.getTotalWorkHours()).isEqualTo(BigDecimal.valueOf(45));
        assertThat(weeklyAllowance.getWeeklyPaidLeaveAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(weeklyAllowance.getOvertimeHours()).isEqualTo(BigDecimal.valueOf(5));
        assertThat(weeklyAllowance.getOvertimeAmount()).isEqualByComparingTo(new BigDecimal("75000"));
    }
}
