package com.example.paycheck.domain.salary.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.salary.dto.SalaryDto;
import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.salary.util.DeductionCalculator;
import com.example.paycheck.domain.workplace.entity.Workplace;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Mock
    private WeeklyAllowanceRepository weeklyAllowanceRepository;

    @Mock
    private SalaryPersistenceService salaryPersistenceService;

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

    @Test
    @DisplayName("월급날이 말일을 초과하면 해당 월의 마지막 날로 보정된다")
    void calculateSalaryByWorkRecords_AdjustsPaymentDayWhenExceedingMonthLength() {
        // given
        Long contractId = 2L;
        Integer year = 2024;
        Integer month = 2;

        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getId()).thenReturn(contractId);
        when(contract.getPaymentDay()).thenReturn(31);
        when(contract.getPayrollDeductionType()).thenReturn(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        User workerUser = mock(User.class);
        when(workerUser.getName()).thenReturn("홍길동");

        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(100L);
        when(worker.getUser()).thenReturn(workerUser);

        Workplace workplace = mock(Workplace.class);
        when(workplace.getId()).thenReturn(200L);
        when(workplace.getName()).thenReturn("테스트 사업장");

        when(contract.getWorker()).thenReturn(worker);
        when(contract.getWorkplace()).thenReturn(workplace);

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        LocalDate febStart = LocalDate.of(2024, 1, 31);
        LocalDate febEnd = LocalDate.of(2024, 2, 29).minusDays(1);

        WorkRecord workRecord = mock(WorkRecord.class);
        when(workRecord.getTotalHours()).thenReturn(BigDecimal.ONE);
        when(workRecord.getBaseSalary()).thenReturn(BigDecimal.ONE);
        when(workRecord.getNightSalary()).thenReturn(BigDecimal.ZERO);
        when(workRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);

        when(workRecordRepository.findByContractAndDateRange(eq(contractId), eq(febStart), eq(febEnd)))
                .thenReturn(Collections.singletonList(workRecord));

        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(salaryRepository.findByContractIdAndYearAndMonth(contractId, year, month))
                .thenReturn(Collections.emptyList());
        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenReturn(Optional.of(Salary.builder()
                        .id(100L)
                        .contract(contract)
                        .year(year)
                        .month(month)
                        .totalWorkHours(BigDecimal.ONE)
                        .basePay(BigDecimal.ONE)
                        .overtimePay(BigDecimal.ZERO)
                        .nightPay(BigDecimal.ZERO)
                        .holidayPay(BigDecimal.ZERO)
                        .totalGrossPay(BigDecimal.ONE)
                        .fourMajorInsurance(BigDecimal.ZERO)
                        .incomeTax(BigDecimal.ZERO)
                        .localIncomeTax(BigDecimal.ZERO)
                        .totalDeduction(BigDecimal.ZERO)
                        .netPay(BigDecimal.ONE)
                        .paymentDueDate(LocalDate.of(2024, 2, 29))
                        .build()));
        when(salaryPersistenceService.trySave(any(Salary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        verify(workRecordRepository).findByContractAndDateRange(eq(contractId), eq(febStart), eq(febEnd));
        verify(weeklyAllowanceRepository).findByContractIdAndYearMonth(eq(contractId), eq(2024), eq(2));
        verify(salaryPersistenceService).trySave(argThat(saved ->
                saved.getPaymentDueDate().equals(LocalDate.of(2024, 2, 29))));
    }

    @Test
    @DisplayName("기존 급여가 있으면 save 없이 필드만 갱신한다")
    void calculateSalaryByWorkRecords_UpdatesExistingSalaryWithoutSave() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 5;

        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getId()).thenReturn(contractId);
        when(contract.getPaymentDay()).thenReturn(10);
        when(contract.getPayrollDeductionType()).thenReturn(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        User workerUser = mock(User.class);
        when(workerUser.getName()).thenReturn("홍길동");

        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(100L);
        when(worker.getUser()).thenReturn(workerUser);

        Workplace workplace = mock(Workplace.class);
        when(workplace.getId()).thenReturn(200L);
        when(workplace.getName()).thenReturn("테스트 사업장");

        when(contract.getWorker()).thenReturn(worker);
        when(contract.getWorkplace()).thenReturn(workplace);

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        WorkRecord workRecord = mock(WorkRecord.class);
        when(workRecord.getTotalHours()).thenReturn(new BigDecimal("8"));
        when(workRecord.getBaseSalary()).thenReturn(new BigDecimal("100"));
        when(workRecord.getNightSalary()).thenReturn(new BigDecimal("20"));
        when(workRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);

        when(workRecordRepository.findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(workRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        Salary existingSalary = Salary.builder()
                .id(10L)
                .contract(contract)
                .year(year)
                .month(month)
                .totalWorkHours(BigDecimal.ZERO)
                .basePay(BigDecimal.ZERO)
                .overtimePay(BigDecimal.ZERO)
                .nightPay(BigDecimal.ZERO)
                .holidayPay(BigDecimal.ZERO)
                .totalGrossPay(BigDecimal.ZERO)
                .fourMajorInsurance(BigDecimal.ZERO)
                .incomeTax(BigDecimal.ZERO)
                .localIncomeTax(BigDecimal.ZERO)
                .totalDeduction(BigDecimal.ZERO)
                .netPay(BigDecimal.ZERO)
                .paymentDueDate(LocalDate.of(2024, 5, 10))
                .build();

        when(salaryRepository.findByContractIdAndYearAndMonth(contractId, year, month))
                .thenReturn(Collections.singletonList(existingSalary));
        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenReturn(Optional.of(existingSalary));

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        verify(salaryRepository, never()).save(any(Salary.class));
        verify(salaryRepository).findByContractIdAndYearAndMonthForUpdate(contractId, year, month);
        verify(workRecordRepository).findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class));

        assertThat(existingSalary.getPaymentDueDate()).isEqualTo(LocalDate.of(2024, 5, 10));
        assertThat(existingSalary.getTotalWorkHours()).isEqualByComparingTo("8");
        assertThat(existingSalary.getBasePay()).isEqualByComparingTo("100");
        assertThat(existingSalary.getNightPay()).isEqualByComparingTo("20");
        assertThat(existingSalary.getHolidayPay()).isEqualByComparingTo("0");
        assertThat(existingSalary.getTotalGrossPay()).isEqualByComparingTo("120");
        assertThat(existingSalary.getNetPay()).isEqualByComparingTo("120");
    }

    @Test
    @DisplayName("급여 자동 계산 - 주휴/연장 수당 월 경계 이월(마지막 주차 제외)")
    void calculateSalaryByWorkRecords_IncludesPreviousMonthCarryoverExcludesCurrentLastWeek() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 3;

        WorkerContract contract = mock(WorkerContract.class);
        when(contract.getPaymentDay()).thenReturn(25);
        when(contract.getPayrollDeductionType()).thenReturn(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        User workerUser = mock(User.class);
        when(workerUser.getName()).thenReturn("홍길동");

        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(100L);
        when(worker.getUser()).thenReturn(workerUser);

        Workplace workplace = mock(Workplace.class);
        when(workplace.getId()).thenReturn(200L);
        when(workplace.getName()).thenReturn("테스트 사업장");

        when(contract.getWorker()).thenReturn(worker);
        when(contract.getWorkplace()).thenReturn(workplace);
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        LocalDate startDate = LocalDate.of(2024, 2, 25);
        LocalDate endDate = LocalDate.of(2024, 3, 24);

        WorkRecord workRecord = mock(WorkRecord.class);
        when(workRecord.getTotalHours()).thenReturn(new BigDecimal("8.00"));
        when(workRecord.getBaseSalary()).thenReturn(new BigDecimal("100000.00"));
        when(workRecord.getNightSalary()).thenReturn(BigDecimal.ZERO);
        when(workRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);

        when(workRecordRepository.findByContractAndDateRange(eq(contractId), eq(startDate), eq(endDate)))
                .thenReturn(List.of(workRecord));

        WeeklyAllowance includedCurrent = mock(WeeklyAllowance.class);
        when(includedCurrent.getWeekStartDate()).thenReturn(LocalDate.of(2024, 3, 4));
        when(includedCurrent.getWeekEndDate()).thenReturn(LocalDate.of(2024, 3, 10));
        when(includedCurrent.getWeeklyPaidLeaveAmount()).thenReturn(new BigDecimal("30000.00"));
        when(includedCurrent.getOvertimeAmount()).thenReturn(new BigDecimal("12000.00"));

        WeeklyAllowance excludedCurrentLastWeek = mock(WeeklyAllowance.class);
        when(excludedCurrentLastWeek.getWeekStartDate()).thenReturn(LocalDate.of(2024, 3, 25));
        when(excludedCurrentLastWeek.getWeekEndDate()).thenReturn(LocalDate.of(2024, 3, 31));

        WeeklyAllowance includedPreviousLastWeek = mock(WeeklyAllowance.class);
        when(includedPreviousLastWeek.getWeekStartDate()).thenReturn(LocalDate.of(2024, 2, 19));
        when(includedPreviousLastWeek.getWeekEndDate()).thenReturn(LocalDate.of(2024, 2, 25));
        when(includedPreviousLastWeek.getWeeklyPaidLeaveAmount()).thenReturn(new BigDecimal("40000.00"));
        when(includedPreviousLastWeek.getOvertimeAmount()).thenReturn(new BigDecimal("10000.00"));

        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(contractId, year, month))
                .thenReturn(List.of(includedCurrent, excludedCurrentLastWeek));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(contractId, 2024, 2))
                .thenReturn(List.of(includedPreviousLastWeek));

        AtomicReference<Salary> persistedSalary = new AtomicReference<>();

        when(salaryRepository.findByContractIdAndYearAndMonth(contractId, year, month))
                .thenReturn(Collections.emptyList());
        when(salaryPersistenceService.trySave(any(Salary.class))).thenAnswer(invocation -> {
            Salary saved = invocation.getArgument(0);
            persistedSalary.set(saved);
            return saved;
        });
        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenAnswer(invocation -> Optional.ofNullable(persistedSalary.get()));

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        assertThat(response.getTotalGrossPay()).isEqualByComparingTo(new BigDecimal("192000.00"));
        assertThat(response.getOvertimePay()).isEqualByComparingTo(new BigDecimal("22000.00"));
        assertThat(response.getPaymentDueDate()).isEqualTo("2024-03-25");

        verify(workRecordRepository).findByContractAndDateRange(eq(contractId), eq(startDate), eq(endDate));
        verify(weeklyAllowanceRepository).findByContractIdAndYearMonth(contractId, year, month);
        verify(weeklyAllowanceRepository).findByContractIdAndYearMonth(contractId, 2024, 2);
    }
}
