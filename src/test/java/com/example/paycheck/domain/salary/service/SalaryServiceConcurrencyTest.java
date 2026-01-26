package com.example.paycheck.domain.salary.service;

import com.example.paycheck.domain.allowance.repository.WeeklyAllowanceRepository;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.contract.repository.WorkerContractRepository;
import com.example.paycheck.domain.salary.dto.SalaryDto;
import com.example.paycheck.domain.salary.entity.Salary;
import com.example.paycheck.domain.salary.repository.SalaryRepository;
import com.example.paycheck.domain.salary.util.DeductionCalculator;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workplace.entity.Workplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @InjectMocks
    private SalaryService salaryService;

    private WorkerContract mockContract;
    private WorkRecord mockWorkRecord;
    private Worker mockWorker;
    private User mockUser;
    private Workplace mockWorkplace;

    @BeforeEach
    void setUp() {
        // User mock
        mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("테스트 근로자");

        // Worker mock
        mockWorker = mock(Worker.class);
        when(mockWorker.getId()).thenReturn(1L);
        when(mockWorker.getUser()).thenReturn(mockUser);

        // Workplace mock
        mockWorkplace = mock(Workplace.class);
        when(mockWorkplace.getId()).thenReturn(1L);
        when(mockWorkplace.getName()).thenReturn("테스트 사업장");

        // Contract mock
        mockContract = mock(WorkerContract.class);
        when(mockContract.getId()).thenReturn(1L);
        when(mockContract.getPaymentDay()).thenReturn(25);
        when(mockContract.getPayrollDeductionType()).thenReturn(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE);
        when(mockContract.getWorker()).thenReturn(mockWorker);
        when(mockContract.getWorkplace()).thenReturn(mockWorkplace);

        // WorkRecord mock
        mockWorkRecord = mock(WorkRecord.class);
        when(mockWorkRecord.getTotalHours()).thenReturn(BigDecimal.valueOf(8));
        when(mockWorkRecord.getBaseSalary()).thenReturn(BigDecimal.valueOf(80000));
        when(mockWorkRecord.getNightSalary()).thenReturn(BigDecimal.ZERO);
        when(mockWorkRecord.getHolidaySalary()).thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("급여 계산 시 비관적 잠금 쿼리 호출 확인")
    void calculateSalary_UsesPessimisticLockQuery() {
        // given
        Long contractId = 1L;
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(anyLong(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList());
        when(salaryRepository.findByContractIdAndYearAndMonthWithLock(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(salaryRepository.saveAndFlush(any(Salary.class))).thenAnswer(i -> {
            Salary saved = i.getArgument(0);
            return createSalaryWithId(saved, 1L);
        });

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1);

        // then - 비관적 잠금 쿼리가 호출되었는지 확인
        verify(salaryRepository).findByContractIdAndYearAndMonthWithLock(contractId, 2024, 1);
        // 기존 락 없는 쿼리는 호출되지 않음
        verify(salaryRepository, never()).findByContractIdAndYearAndMonth(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("새 급여 생성 시 saveAndFlush 사용 확인")
    void calculateSalary_NewSalary_UsesSaveAndFlush() {
        // given
        Long contractId = 1L;
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(anyLong(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList());
        when(salaryRepository.findByContractIdAndYearAndMonthWithLock(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(salaryRepository.saveAndFlush(any(Salary.class))).thenAnswer(i -> {
            Salary saved = i.getArgument(0);
            return createSalaryWithId(saved, 1L);
        });

        // when
        salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1);

        // then - saveAndFlush가 호출되었는지 확인 (중복 즉시 감지용)
        verify(salaryRepository).saveAndFlush(any(Salary.class));
        // 일반 save는 호출되지 않음
        verify(salaryRepository, never()).save(any(Salary.class));
    }

    @Test
    @DisplayName("기존 급여 업데이트 시 updateCalculation 메서드 사용 확인")
    void calculateSalary_ExistingSalary_UsesUpdateCalculation() {
        // given
        Long contractId = 1L;
        Salary existingSalary = spy(Salary.builder()
                .id(100L)
                .contract(mockContract)
                .year(2024)
                .month(1)
                .totalWorkHours(BigDecimal.valueOf(8))
                .basePay(BigDecimal.valueOf(80000))
                .overtimePay(BigDecimal.ZERO)
                .nightPay(BigDecimal.ZERO)
                .holidayPay(BigDecimal.ZERO)
                .totalGrossPay(BigDecimal.valueOf(80000))
                .fourMajorInsurance(BigDecimal.ZERO)
                .incomeTax(BigDecimal.ZERO)
                .localIncomeTax(BigDecimal.ZERO)
                .totalDeduction(BigDecimal.ZERO)
                .netPay(BigDecimal.valueOf(80000))
                .paymentDueDate(LocalDate.of(2024, 1, 25))
                .build());

        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(anyLong(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList());
        when(salaryRepository.findByContractIdAndYearAndMonthWithLock(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.of(existingSalary));

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1);

        // then - updateCalculation이 호출되었는지 확인 (JPA 추적 유지)
        verify(existingSalary).updateCalculation(
                any(BigDecimal.class),  // totalWorkHours
                any(BigDecimal.class),  // basePay
                any(BigDecimal.class),  // overtimePay
                any(BigDecimal.class),  // nightPay
                any(BigDecimal.class),  // holidayPay
                any(BigDecimal.class),  // totalGrossPay
                any(BigDecimal.class),  // fourMajorInsurance
                any(BigDecimal.class),  // incomeTax
                any(BigDecimal.class),  // localIncomeTax
                any(BigDecimal.class),  // totalDeduction
                any(BigDecimal.class)   // netPay
        );
        // 새 엔티티 저장은 호출되지 않음 (dirty checking으로 자동 저장)
        verify(salaryRepository, never()).save(any(Salary.class));
        verify(salaryRepository, never()).saveAndFlush(any(Salary.class));
    }

    @Test
    @DisplayName("급여 계산 결과가 정확히 반환되는지 확인")
    void calculateSalary_ReturnsCorrectResponse() {
        // given
        Long contractId = 1L;
        when(workerContractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(workRecordRepository.findByContractAndDateRange(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(mockWorkRecord));
        when(weeklyAllowanceRepository.findByContractIdAndYearMonth(anyLong(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList());
        when(salaryRepository.findByContractIdAndYearAndMonthWithLock(anyLong(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());
        when(salaryRepository.saveAndFlush(any(Salary.class))).thenAnswer(i -> {
            Salary saved = i.getArgument(0);
            return createSalaryWithId(saved, 1L);
        });

        // when
        SalaryDto.Response response = salaryService.calculateSalaryByWorkRecords(contractId, 2024, 1);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo(2024);
        assertThat(response.getMonth()).isEqualTo(1);
        assertThat(response.getBasePay()).isEqualByComparingTo(BigDecimal.valueOf(80000));
    }

    /**
     * 저장된 Salary에 ID를 부여하여 반환하는 헬퍼 메서드
     */
    private Salary createSalaryWithId(Salary source, Long id) {
        return Salary.builder()
                .id(id)
                .contract(source.getContract())
                .year(source.getYear())
                .month(source.getMonth())
                .totalWorkHours(source.getTotalWorkHours())
                .basePay(source.getBasePay())
                .overtimePay(source.getOvertimePay())
                .nightPay(source.getNightPay())
                .holidayPay(source.getHolidayPay())
                .totalGrossPay(source.getTotalGrossPay())
                .fourMajorInsurance(source.getFourMajorInsurance())
                .incomeTax(source.getIncomeTax())
                .localIncomeTax(source.getLocalIncomeTax())
                .totalDeduction(source.getTotalDeduction())
                .netPay(source.getNetPay())
                .paymentDueDate(source.getPaymentDueDate())
                .build();
    }
}
