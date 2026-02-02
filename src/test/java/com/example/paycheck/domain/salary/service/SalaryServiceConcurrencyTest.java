package com.example.paycheck.domain.salary.service;

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
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryService 동시성 제어 테스트")
class SalaryServiceConcurrencyTest {

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

    private WorkerContract mockContract;
    private WorkRecord mockWorkRecord;

    @BeforeEach
    void setUp() {
        // Mock Contract 설정
        mockContract = mock(WorkerContract.class);
        when(mockContract.getId()).thenReturn(1L);
        when(mockContract.getPaymentDay()).thenReturn(25);
        when(mockContract.getPayrollDeductionType()).thenReturn(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);

        User workerUser = mock(User.class);
        when(workerUser.getName()).thenReturn("테스트 근로자");

        Worker worker = mock(Worker.class);
        when(worker.getId()).thenReturn(100L);
        when(worker.getUser()).thenReturn(workerUser);

        Workplace workplace = mock(Workplace.class);
        when(workplace.getId()).thenReturn(200L);
        when(workplace.getName()).thenReturn("테스트 사업장");

        when(mockContract.getWorker()).thenReturn(worker);
        when(mockContract.getWorkplace()).thenReturn(workplace);

        // Mock WorkRecord 설정
        mockWorkRecord = mock(WorkRecord.class);
        when(mockWorkRecord.getTotalHours()).thenReturn(new BigDecimal("8"));
        when(mockWorkRecord.getBaseSalary()).thenReturn(new BigDecimal("100000"));
        when(mockWorkRecord.getNightSalary()).thenReturn(BigDecimal.ZERO);
        when(mockWorkRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("비관적 잠금 메서드를 통해 기존 급여를 조회한다")
    void calculateSalaryByWorkRecords_UsesPessimisticLock() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 5;

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        Salary existingSalary = Salary.builder()
                .id(10L)
                .contract(mockContract)
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
                .paymentDueDate(LocalDate.of(2024, 5, 25))
                .build();

        // 비관적 잠금 메서드가 호출되어야 함
        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenReturn(Optional.of(existingSalary));

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        verify(salaryRepository).findByContractIdAndYearAndMonthForUpdate(contractId, year, month);
        verify(salaryPersistenceService, never()).trySave(any(Salary.class));
    }

    @Test
    @DisplayName("신규 급여 생성 시 동시 INSERT로 인한 Unique 제약 위반 시 재조회 후 업데이트한다")
    void calculateSalaryByWorkRecords_HandlesDataIntegrityViolation() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 5;

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // 첫 번째 조회: 기존 급여 없음
        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(Salary.builder()
                        .id(10L)
                        .contract(mockContract)
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
                        .paymentDueDate(LocalDate.of(2024, 5, 25))
                        .build()));

        // trySave 시 DataIntegrityViolationException 발생 (동시 INSERT)
        when(salaryPersistenceService.trySave(any(Salary.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        // 첫 번째 조회 + 예외 발생 후 재조회 = 총 2번 호출
        verify(salaryRepository, times(2)).findByContractIdAndYearAndMonthForUpdate(contractId, year, month);
        verify(salaryPersistenceService).trySave(any(Salary.class));
    }

    @Test
    @DisplayName("신규 급여 생성 성공 시 save가 호출된다")
    void calculateSalaryByWorkRecords_SavesNewSalary() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 5;

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        // 기존 급여 없음
        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenReturn(Optional.empty());

        when(salaryPersistenceService.trySave(any(Salary.class))).thenAnswer(invocation -> {
            Salary saved = invocation.getArgument(0);
            return Salary.builder()
                    .id(10L)
                    .contract(saved.getContract())
                    .year(saved.getYear())
                    .month(saved.getMonth())
                    .totalWorkHours(saved.getTotalWorkHours())
                    .basePay(saved.getBasePay())
                    .overtimePay(saved.getOvertimePay())
                    .nightPay(saved.getNightPay())
                    .holidayPay(saved.getHolidayPay())
                    .totalGrossPay(saved.getTotalGrossPay())
                    .fourMajorInsurance(saved.getFourMajorInsurance())
                    .incomeTax(saved.getIncomeTax())
                    .localIncomeTax(saved.getLocalIncomeTax())
                    .totalDeduction(saved.getTotalDeduction())
                    .netPay(saved.getNetPay())
                    .paymentDueDate(saved.getPaymentDueDate())
                    .build();
        });

        // when
        var response = salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        verify(salaryRepository).findByContractIdAndYearAndMonthForUpdate(contractId, year, month);
        verify(salaryPersistenceService).trySave(any(Salary.class));
        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo(year);
        assertThat(response.getMonth()).isEqualTo(month);
    }

    @Test
    @DisplayName("기존 급여 업데이트 시 save가 호출되지 않는다")
    void calculateSalaryByWorkRecords_UpdatesExistingSalaryWithoutSave() {
        // given
        Long contractId = 1L;
        Integer year = 2024;
        Integer month = 5;

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(eq(contractId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(eq(contractId), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        Salary existingSalary = Salary.builder()
                .id(10L)
                .contract(mockContract)
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
                .paymentDueDate(LocalDate.of(2024, 5, 25))
                .build();

        when(salaryRepository.findByContractIdAndYearAndMonthForUpdate(contractId, year, month))
                .thenReturn(Optional.of(existingSalary));

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, year, month);

        // then
        verify(salaryRepository).findByContractIdAndYearAndMonthForUpdate(contractId, year, month);
        verify(salaryPersistenceService, never()).trySave(any(Salary.class));

        // 업데이트된 값 확인
        assertThat(existingSalary.getTotalWorkHours()).isEqualByComparingTo("8");
        assertThat(existingSalary.getBasePay()).isEqualByComparingTo("100000");
    }
}
