package com.example.wagemanager.domain.salary.service;

import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
import com.example.wagemanager.domain.salary.entity.Salary;
import com.example.wagemanager.domain.salary.repository.SalaryRepository;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryService 간단 테스트")
class SalaryServiceSimpleTest {

    @Mock
    private SalaryRepository salaryRepository;

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @InjectMocks
    private SalaryService salaryService;

    private Salary testSalary;
    private WorkerContract testContract;

    @BeforeEach
    void setUp() {
        testContract = WorkerContract.builder()
                .id(1L)
                .paymentDay(25)
                .build();

        testSalary = Salary.builder()
                .id(1L)
                .contract(testContract)
                .year(2024)
                .month(12)
                .totalWorkHours(BigDecimal.valueOf(160))
                .basePay(BigDecimal.valueOf(1500000))
                .totalGrossPay(BigDecimal.valueOf(1730000))
                .netPay(BigDecimal.valueOf(1547000))
                .paymentDueDate(LocalDate.of(2024, 12, 25))
                .build();
    }

    @Test
    @DisplayName("급여 ID로 조회 실패 - 존재하지 않는 급여")
    void getSalaryById_NotFound() {
        // given
        when(salaryRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> salaryService.getSalaryById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("급여 정보를 찾을 수 없습니다");

        verify(salaryRepository).findById(999L);
    }

    @Test
    @DisplayName("급여 자동 계산 실패 - 계약을 찾을 수 없음")
    void calculateSalaryByWorkRecords_ContractNotFound() {
        // given
        Long contractId = 999L;
        Integer year = 2024;
        Integer month = 12;
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, year, month))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("계약을 찾을 수 없습니다");

        verify(workerContractRepository).findById(contractId);
    }

    @Test
    @DisplayName("급여 자동 계산 실패 - 근무 기록이 없음")
    void calculateSalaryByWorkRecords_NoWorkRecords() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 12;

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(testContract));
        when(workRecordRepository.findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, year, month))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("해당 기간 내 근무 기록이 없습니다");

        verify(workerContractRepository).findById(contractId);
        verify(workRecordRepository).findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class));
    }
}
