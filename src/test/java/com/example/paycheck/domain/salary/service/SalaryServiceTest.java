package com.example.paycheck.domain.salary.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryService 테스트")
class SalaryServiceTest {

    @Mock
    private SalaryRepository salaryRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private WeeklyAllowanceRepository weeklyAllowanceRepository;

    @InjectMocks
    private SalaryService salaryService;

    @Test
    @DisplayName("급여 상세 조회 실패 - 급여 없음")
    void getSalaryById_Fail_NotFound() {
        // given
        when(salaryRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> salaryService.getSalaryById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("급여 정보를 찾을 수 없습니다");
        verify(salaryRepository).findById(999L);
    }

    @Test
    @DisplayName("사업장별 월별 급여 목록 조회")
    void getSalariesByWorkplace_Success() {
        // given
        Long workplaceId = 1L;
        when(salaryRepository.findByWorkplaceId(workplaceId)).thenReturn(Arrays.asList());

        // when
        salaryService.getSalariesByWorkplace(workplaceId);

        // then
        verify(salaryRepository).findByWorkplaceId(workplaceId);
    }

    @Test
    @DisplayName("사업장별 연월 급여 목록 조회")
    void getSalariesByWorkplaceAndYearMonth_Success() {
        // given
        Long workplaceId = 1L;
        Integer year = 2024;
        Integer month = 1;
        when(salaryRepository.findByWorkplaceId(workplaceId)).thenReturn(Arrays.asList());

        // when
        salaryService.getSalariesByWorkplaceAndYearMonth(workplaceId, year, month);

        // then
        verify(salaryRepository).findByWorkplaceId(workplaceId);
    }

    @Test
    @DisplayName("근로자별 급여 목록 조회")
    void getSalariesByWorker_Success() {
        // given
        Long workerId = 1L;
        when(salaryRepository.findByWorkerId(workerId)).thenReturn(Arrays.asList());

        // when
        salaryService.getSalariesByWorker(workerId);

        // then
        verify(salaryRepository).findByWorkerId(workerId);
    }

    @Test
    @DisplayName("계약별 급여 목록 조회")
    void getSalariesByContract_Success() {
        // given
        Long contractId = 1L;
        when(salaryRepository.findByContractId(contractId)).thenReturn(Arrays.asList());

        // when
        salaryService.getSalariesByContract(contractId);

        // then
        verify(salaryRepository).findByContractId(contractId);
    }

    @Test
    @DisplayName("급여 자동 계산 실패 - 계약 없음")
    void calculateSalaryByWorkRecords_Fail_ContractNotFound() {
        // given
        Long contractId = 999L;
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("계약을 찾을 수 없습니다");
        verify(workerContractRepository).findById(contractId);
    }

    @Test
    @DisplayName("급여 자동 계산 실패 - 근무 기록 없음")
    void calculateSalaryByWorkRecords_Fail_NoWorkRecords() {
        // given
        Long contractId = 1L;
        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getPaymentDay()).thenReturn(25);
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(workRecordRepository.findByContractAndDateRange(anyLong(), any(), any(), any(WorkRecordStatus.class)))
                .thenReturn(Arrays.asList());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("해당 기간 내 근무 기록이 없습니다");
        verify(workRecordRepository).findByContractAndDateRange(anyLong(), any(), any(), any(WorkRecordStatus.class));
    }

    @Test
    @DisplayName("급여 재계산 호출")
    void recalculateSalaryAfterWorkRecordUpdate_CallsCalculate() {
        // given
        Long contractId = 1L;
        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getPaymentDay()).thenReturn(25);
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(workRecordRepository.findByContractAndDateRange(anyLong(), any(), any(), any(WorkRecordStatus.class)))
                .thenReturn(Arrays.asList());

        // when & then
        assertThatThrownBy(() -> salaryService.recalculateSalaryAfterWorkRecordUpdate(contractId, 2024, 1))
                .isInstanceOf(NotFoundException.class);
        verify(workerContractRepository).findById(contractId);
    }
}
