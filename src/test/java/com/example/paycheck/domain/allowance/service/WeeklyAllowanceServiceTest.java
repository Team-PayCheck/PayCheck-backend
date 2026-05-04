package com.example.paycheck.domain.allowance.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAllowanceService 테스트")
class WeeklyAllowanceServiceTest {

    @Mock
    private WeeklyAllowanceRepository weeklyAllowanceRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

    @InjectMocks
    private WeeklyAllowanceService weeklyAllowanceService;

    @Test
    @DisplayName("계약별 주간 수당 목록 조회")
    void getWeeklyAllowancesByContract_Success() {
        // given
        when(weeklyAllowanceRepository.findByContractId(1L)).thenReturn(Arrays.asList());

        // when
        List<WeeklyAllowance> result = weeklyAllowanceService.getWeeklyAllowancesByContract(1L);

        // then
        assertThat(result).isNotNull();
        verify(weeklyAllowanceRepository).findByContractId(1L);
    }

    @Test
    @DisplayName("주간 수당 조회 또는 생성 - 기존 존재")
    void getOrCreateWeeklyAllowanceForDate_Existing() {
        // given
        WeeklyAllowance existingAllowance = mock(WeeklyAllowance.class);
        LocalDate workDate = LocalDate.of(2024, 1, 15);

        when(weeklyAllowanceRepository.findByContractAndWeek(1L, workDate))
                .thenReturn(Optional.of(existingAllowance));

        // when
        WeeklyAllowance result = weeklyAllowanceService.getOrCreateWeeklyAllowanceForDate(1L, workDate);

        // then
        assertThat(result).isEqualTo(existingAllowance);
        verify(weeklyAllowanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("주간 수당 조회 또는 생성 - 신규 생성")
    void getOrCreateWeeklyAllowanceForDate_New() {
        // given
        WorkerContract contract = mock(WorkerContract.class);
        LocalDate workDate = LocalDate.of(2024, 1, 15);
        WeeklyAllowance newAllowance = mock(WeeklyAllowance.class);

        when(weeklyAllowanceRepository.findByContractAndWeek(1L, workDate))
                .thenReturn(Optional.empty());
        when(workerContractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(weeklyAllowanceRepository.save(any(WeeklyAllowance.class))).thenReturn(newAllowance);

        // when
        WeeklyAllowance result = weeklyAllowanceService.getOrCreateWeeklyAllowanceForDate(1L, workDate);

        // then
        assertThat(result).isEqualTo(newAllowance);
        verify(weeklyAllowanceRepository).save(any(WeeklyAllowance.class));
    }

    @Test
    @DisplayName("주간 수당 생성 실패 - 계약 없음")
    void getOrCreateWeeklyAllowanceForDate_Fail_ContractNotFound() {
        // given
        LocalDate workDate = LocalDate.of(2024, 1, 15);

        when(weeklyAllowanceRepository.findByContractAndWeek(1L, workDate))
                .thenReturn(Optional.empty());
        when(workerContractRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> weeklyAllowanceService.getOrCreateWeeklyAllowanceForDate(1L, workDate))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("수당 재계산 성공")
    void recalculateAllowances_Success() {
        // given
        WeeklyAllowance allowance = mock(WeeklyAllowance.class);
        WorkerContract mockContract = mock(WorkerContract.class);
        com.example.paycheck.domain.workplace.entity.Workplace mockWorkplace = mock(com.example.paycheck.domain.workplace.entity.Workplace.class);

        when(allowance.getContract()).thenReturn(mockContract);
        when(allowance.getWeekEndDate()).thenReturn(LocalDate.of(2024, 1, 7));
        when(mockContract.getId()).thenReturn(1L);
        when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
        when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);

        when(weeklyAllowanceRepository.findById(1L)).thenReturn(Optional.of(allowance));
        when(weeklyAllowanceRepository.save(allowance)).thenReturn(allowance);
        when(workRecordRepository.existsByContractIdAndWorkDateBetweenAndStatusNot(
                eq(1L), any(LocalDate.class), any(LocalDate.class), eq(WorkRecordStatus.DELETED)))
                .thenReturn(true);
        doNothing().when(allowance).calculateTotalWorkHours();
        doNothing().when(allowance).calculateWeeklyPaidLeave(anyBoolean());
        doNothing().when(allowance).calculateOvertime(anyBoolean());

        // when
        WeeklyAllowance result = weeklyAllowanceService.recalculateAllowances(1L);

        // then
        assertThat(result).isEqualTo(allowance);
        verify(allowance).calculateTotalWorkHours();
        verify(allowance).calculateWeeklyPaidLeave(anyBoolean());
        verify(allowance).calculateOvertime(anyBoolean());
    }

    @Test
    @DisplayName("수당 재계산 실패 - 주간 수당 없음")
    void recalculateAllowances_Fail_NotFound() {
        // given
        when(weeklyAllowanceRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> weeklyAllowanceService.recalculateAllowances(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Nested
    @DisplayName("주휴수당 다음 주 근무 기록 기반 판단 테스트")
    class WeeklyPaidLeaveNextWeekTest {

        @Test
        @DisplayName("다음 주 근무 기록 있을 때 주휴수당 정상 계산")
        void recalculateAllowances_HasNextWeekRecords_CalculatesNormally() {
            // given
            WeeklyAllowance allowance = mock(WeeklyAllowance.class);
            WorkerContract mockContract = mock(WorkerContract.class);
            com.example.paycheck.domain.workplace.entity.Workplace mockWorkplace = mock(com.example.paycheck.domain.workplace.entity.Workplace.class);

            when(allowance.getContract()).thenReturn(mockContract);
            when(allowance.getWeekEndDate()).thenReturn(LocalDate.of(2024, 1, 7));
            when(mockContract.getId()).thenReturn(1L);
            when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);

            when(weeklyAllowanceRepository.findById(1L)).thenReturn(Optional.of(allowance));
            when(weeklyAllowanceRepository.save(allowance)).thenReturn(allowance);
            when(workRecordRepository.existsByContractIdAndWorkDateBetweenAndStatusNot(
                    eq(1L), eq(LocalDate.of(2024, 1, 8)), eq(LocalDate.of(2024, 1, 14)), eq(WorkRecordStatus.DELETED)))
                    .thenReturn(true);

            // when
            weeklyAllowanceService.recalculateAllowances(1L);

            // then
            verify(allowance).calculateWeeklyPaidLeave(true);
        }

        @Test
        @DisplayName("다음 주 근무 기록 없을 때 주휴수당 미지급으로 계산")
        void recalculateAllowances_NoNextWeekRecords_ZeroPay() {
            // given
            WeeklyAllowance allowance = mock(WeeklyAllowance.class);
            WorkerContract mockContract = mock(WorkerContract.class);
            com.example.paycheck.domain.workplace.entity.Workplace mockWorkplace = mock(com.example.paycheck.domain.workplace.entity.Workplace.class);

            when(allowance.getContract()).thenReturn(mockContract);
            when(allowance.getWeekEndDate()).thenReturn(LocalDate.of(2024, 1, 7));
            when(mockContract.getId()).thenReturn(1L);
            when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);

            when(weeklyAllowanceRepository.findById(1L)).thenReturn(Optional.of(allowance));
            when(weeklyAllowanceRepository.save(allowance)).thenReturn(allowance);
            when(workRecordRepository.existsByContractIdAndWorkDateBetweenAndStatusNot(
                    eq(1L), eq(LocalDate.of(2024, 1, 8)), eq(LocalDate.of(2024, 1, 14)), eq(WorkRecordStatus.DELETED)))
                    .thenReturn(false);

            // when
            weeklyAllowanceService.recalculateAllowances(1L);

            // then
            verify(allowance).calculateWeeklyPaidLeave(false);
        }

        @Test
        @DisplayName("다음 주 근무 기록이 모두 DELETED이면 주휴수당 미지급")
        void recalculateAllowances_AllDeletedNextWeekRecords_ZeroPay() {
            // given
            WeeklyAllowance allowance = mock(WeeklyAllowance.class);
            WorkerContract mockContract = mock(WorkerContract.class);
            com.example.paycheck.domain.workplace.entity.Workplace mockWorkplace = mock(com.example.paycheck.domain.workplace.entity.Workplace.class);

            when(allowance.getContract()).thenReturn(mockContract);
            when(allowance.getWeekEndDate()).thenReturn(LocalDate.of(2024, 1, 7));
            when(mockContract.getId()).thenReturn(1L);
            when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
            when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);

            when(weeklyAllowanceRepository.findById(1L)).thenReturn(Optional.of(allowance));
            when(weeklyAllowanceRepository.save(allowance)).thenReturn(allowance);
            // DELETED 상태만 있으면 existsBy...StatusNot은 false 반환
            when(workRecordRepository.existsByContractIdAndWorkDateBetweenAndStatusNot(
                    eq(1L), eq(LocalDate.of(2024, 1, 8)), eq(LocalDate.of(2024, 1, 14)), eq(WorkRecordStatus.DELETED)))
                    .thenReturn(false);

            // when
            weeklyAllowanceService.recalculateAllowances(1L);

            // then
            verify(allowance).calculateWeeklyPaidLeave(false);
        }
    }

    @Nested
    @DisplayName("WeeklyAllowance 엔티티 경계값 테스트")
    class WeeklyAllowanceBoundaryTest {

        private WeeklyAllowance createAllowance(BigDecimal totalWorkHours, BigDecimal hourlyWage) {
            WorkerContract contract = mock(WorkerContract.class);
            lenient().when(contract.getHourlyWage()).thenReturn(hourlyWage);

            return WeeklyAllowance.builder()
                    .contract(contract)
                    .totalWorkHours(totalWorkHours)
                    .weekStartDate(LocalDate.of(2024, 1, 15))
                    .weekEndDate(LocalDate.of(2024, 1, 21))
                    .build();
        }

        @Test
        @DisplayName("주휴수당 - 정확히 15시간 근무 시 지급 (경계 포함)")
        void calculateWeeklyPaidLeave_Exactly15Hours_ShouldPay() {
            // given
            WeeklyAllowance allowance = createAllowance(
                    new BigDecimal("15.00"), new BigDecimal("10000"));

            // when
            allowance.calculateWeeklyPaidLeave(true);

            // then
            // 15 / 40 = 0.38 (HALF_UP) → 0.38 * 8 = 3.04 → 3.04 * 10000 = 30400
            assertThat(allowance.getWeeklyPaidLeaveAmount())
                    .isEqualByComparingTo(new BigDecimal("30400"));
        }

        @Test
        @DisplayName("주휴수당 - 14.99시간 근무 시 미지급 (경계 미만)")
        void calculateWeeklyPaidLeave_14_99Hours_ShouldNotPay() {
            // given
            WeeklyAllowance allowance = createAllowance(
                    new BigDecimal("14.99"), new BigDecimal("10000"));

            // when
            allowance.calculateWeeklyPaidLeave(true);

            // then
            assertThat(allowance.getWeeklyPaidLeaveAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("연장수당 - 정확히 40시간 근무 시 미발생 (경계)")
        void calculateOvertime_Exactly40Hours_ShouldNotPay() {
            // given
            WeeklyAllowance allowance = createAllowance(
                    new BigDecimal("40.00"), new BigDecimal("10000"));

            // when
            allowance.calculateOvertime(false);

            // then
            assertThat(allowance.getOvertimeHours())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(allowance.getOvertimeAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("연장수당 - 40.01시간 근무 시 발생 (경계 초과)")
        void calculateOvertime_40_01Hours_ShouldPay() {
            // given
            WeeklyAllowance allowance = createAllowance(
                    new BigDecimal("40.01"), new BigDecimal("10000"));

            // when
            allowance.calculateOvertime(false);

            // then
            // overtimeHours = 40.01 - 40 = 0.01
            // overtimeAmount = 0.01 * 10000 * 0.5(가산분만) = 50.0
            assertThat(allowance.getOvertimeHours())
                    .isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(allowance.getOvertimeAmount())
                    .isEqualByComparingTo(new BigDecimal("50.0"));
        }

        @Test
        @DisplayName("연장수당 - 40시간 초과 + 5인 미만 사업장 시 미발생")
        void calculateOvertime_Over40Hours_SmallWorkplace_ShouldNotPay() {
            // given
            WeeklyAllowance allowance = createAllowance(
                    new BigDecimal("45.00"), new BigDecimal("10000"));

            // when
            allowance.calculateOvertime(true);

            // then
            assertThat(allowance.getOvertimeHours())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(allowance.getOvertimeAmount())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
