package com.example.wagemanager.domain.allowance.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyAllowanceService 테스트")
class WeeklyAllowanceServiceTest {

    @Mock
    private WeeklyAllowanceRepository weeklyAllowanceRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

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
        com.example.wagemanager.domain.workplace.entity.Workplace mockWorkplace = mock(com.example.wagemanager.domain.workplace.entity.Workplace.class);

        when(allowance.getContract()).thenReturn(mockContract);
        when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
        when(mockWorkplace.getIsLessThanFiveEmployees()).thenReturn(false);

        when(weeklyAllowanceRepository.findById(1L)).thenReturn(Optional.of(allowance));
        when(weeklyAllowanceRepository.save(allowance)).thenReturn(allowance);
        doNothing().when(allowance).calculateTotalWorkHours();
        doNothing().when(allowance).calculateWeeklyPaidLeave();
        doNothing().when(allowance).calculateOvertime(anyBoolean());

        // when
        WeeklyAllowance result = weeklyAllowanceService.recalculateAllowances(1L);

        // then
        assertThat(result).isEqualTo(allowance);
        verify(allowance).calculateTotalWorkHours();
        verify(allowance).calculateWeeklyPaidLeave();
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
}
