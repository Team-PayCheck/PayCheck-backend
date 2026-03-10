package com.example.paycheck.domain.salary.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.salary.util.DeductionCalculator;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.*;
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

    @Mock
    private SalaryPersistenceService salaryPersistenceService;

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
        when(salaryRepository.findByWorkplaceIdAndYearAndMonth(workplaceId, year, month)).thenReturn(Arrays.asList());

        // when
        salaryService.getSalariesByWorkplaceAndYearMonth(workplaceId, year, month);

        // then
        verify(salaryRepository).findByWorkplaceIdAndYearAndMonth(workplaceId, year, month);
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

    // ========================================
    // adjustDayOfMonth 경계값 테스트
    // ========================================

    @Test
    @DisplayName("paymentDay=31 + 윤년 2월 - startDate=1/31, endDate=2/28")
    void calculateSalary_PaymentDay31_LeapYear_February() {
        // given
        // 2024년은 윤년, 2월은 29일까지
        // startDate = adjustDayOfMonth(2024-01-01, 31) = 2024-01-31
        // endDate = adjustDayOfMonth(2024-02-01, 31).minusDays(1) = 2024-02-29 - 1 = 2024-02-28
        Long contractId = 1L;
        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getPaymentDay()).thenReturn(31);
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        when(workRecordRepository.findByContractAndDateRange(
                eq(contractId),
                eq(LocalDate.of(2024, 1, 31)),
                eq(LocalDate.of(2024, 2, 28)),
                eq(WorkRecordStatus.DELETED)))
                .thenReturn(Arrays.asList());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, 2024, 2))
                .isInstanceOf(NotFoundException.class);

        verify(workRecordRepository).findByContractAndDateRange(
                eq(contractId),
                eq(LocalDate.of(2024, 1, 31)),
                eq(LocalDate.of(2024, 2, 28)),
                eq(WorkRecordStatus.DELETED));
    }

    @Test
    @DisplayName("paymentDay=31 + 비윤년 2월 - startDate=1/31, endDate=2/27")
    void calculateSalary_PaymentDay31_NonLeapYear_February() {
        // given
        // 2023년은 비윤년, 2월은 28일까지
        // startDate = adjustDayOfMonth(2023-01-01, 31) = 2023-01-31
        // endDate = adjustDayOfMonth(2023-02-01, 31).minusDays(1) = 2023-02-28 - 1 = 2023-02-27
        Long contractId = 1L;
        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getPaymentDay()).thenReturn(31);
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        when(workRecordRepository.findByContractAndDateRange(
                eq(contractId),
                eq(LocalDate.of(2023, 1, 31)),
                eq(LocalDate.of(2023, 2, 27)),
                eq(WorkRecordStatus.DELETED)))
                .thenReturn(Arrays.asList());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, 2023, 2))
                .isInstanceOf(NotFoundException.class);

        verify(workRecordRepository).findByContractAndDateRange(
                eq(contractId),
                eq(LocalDate.of(2023, 1, 31)),
                eq(LocalDate.of(2023, 2, 27)),
                eq(WorkRecordStatus.DELETED));
    }

    @Test
    @DisplayName("paymentDay=30 + 윤년 2월 - startDate=1/30, endDate=2/28")
    void calculateSalary_PaymentDay30_LeapYear_February() {
        // given
        // 2024년 윤년, 2월은 29일까지
        // startDate = adjustDayOfMonth(2024-01-01, 30) = 2024-01-30
        // endDate = adjustDayOfMonth(2024-02-01, 30).minusDays(1) = 2024-02-29 - 1 = 2024-02-28
        Long contractId = 1L;
        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getPaymentDay()).thenReturn(30);
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        when(workRecordRepository.findByContractAndDateRange(
                eq(contractId),
                eq(LocalDate.of(2024, 1, 30)),
                eq(LocalDate.of(2024, 2, 28)),
                eq(WorkRecordStatus.DELETED)))
                .thenReturn(Arrays.asList());

        // when & then
        assertThatThrownBy(() -> salaryService.calculateSalaryByWorkRecords(contractId, 2024, 2))
                .isInstanceOf(NotFoundException.class);

        verify(workRecordRepository).findByContractAndDateRange(
                eq(contractId),
                eq(LocalDate.of(2024, 1, 30)),
                eq(LocalDate.of(2024, 2, 28)),
                eq(WorkRecordStatus.DELETED));
    }

    @Test
    @DisplayName("급여 0원 + PART_TIME_TAX_AND_INSURANCE → 4대보험 면제(PART_TIME_NONE으로 전환)")
    void calculateSalary_ZeroPay_InsuranceExemption() {
        // given
        Long contractId = 1L;
        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getId()).thenReturn(contractId);
        when(contract.getPaymentDay()).thenReturn(25);
        when(contract.getPayrollDeductionType())
                .thenReturn(DeductionCalculator.PayrollDeductionType.PART_TIME_TAX_AND_INSURANCE);

        // SalaryDto.Response.from()에서 필요한 중첩 객체 설정
        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(1L);
        User user = mock(User.class);
        when(user.getName()).thenReturn("테스트근로자");
        when(worker.getUser()).thenReturn(user);
        when(contract.getWorker()).thenReturn(worker);

        Workplace workplace = mock(Workplace.class);
        when(workplace.getId()).thenReturn(1L);
        when(workplace.getName()).thenReturn("테스트사업장");
        when(contract.getWorkplace()).thenReturn(workplace);

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        // 급여가 모두 0인 WorkRecord 생성
        WorkRecord zeroRecord = mock(WorkRecord.class);
        when(zeroRecord.getTotalHours()).thenReturn(BigDecimal.ZERO);
        when(zeroRecord.getBaseSalary()).thenReturn(BigDecimal.ZERO);
        when(zeroRecord.getNightSalary()).thenReturn(BigDecimal.ZERO);
        when(zeroRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);

        when(workRecordRepository.findByContractAndDateRange(
                eq(contractId), any(LocalDate.class), any(LocalDate.class), eq(WorkRecordStatus.DELETED)))
                .thenReturn(Arrays.asList(zeroRecord));

        // WeeklyAllowance 빈 리스트 반환
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(anyLong(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList());

        // 기존 Salary 없음
        when(salaryRepository.findByContractIdAndYearAndMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Arrays.asList());

        // salaryPersistenceService.trySave 호출 시 인자를 그대로 반환
        when(salaryPersistenceService.trySave(any(Salary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1);

        // then
        // trySave에 전달된 Salary의 공제 관련 필드가 모두 0인지 확인 (PART_TIME_NONE으로 전환됨)
        verify(salaryPersistenceService).trySave(argThat(salary -> {
            // PART_TIME_NONE이면 4대보험, 소득세, 지방소득세 모두 0
            return salary.getFourMajorInsurance().compareTo(BigDecimal.ZERO) == 0
                    && salary.getIncomeTax().compareTo(BigDecimal.ZERO) == 0
                    && salary.getLocalIncomeTax().compareTo(BigDecimal.ZERO) == 0
                    && salary.getTotalDeduction().compareTo(BigDecimal.ZERO) == 0
                    && salary.getNetPay().compareTo(BigDecimal.ZERO) == 0;
        }));
    }
}
