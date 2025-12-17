package com.example.wagemanager.domain.contract.entity;

import com.example.wagemanager.domain.salary.util.DeductionCalculator;
import com.example.wagemanager.domain.worker.entity.Worker;
import com.example.wagemanager.domain.workplace.entity.Workplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WorkerContract 엔티티 테스트")
class WorkerContractTest {

    private WorkerContract contract;
    private Workplace mockWorkplace;
    private Worker mockWorker;

    @BeforeEach
    void setUp() {
        mockWorkplace = mock(Workplace.class);
        mockWorker = mock(Worker.class);

        contract = WorkerContract.builder()
                .id(1L)
                .workplace(mockWorkplace)
                .worker(mockWorker)
                .hourlyWage(BigDecimal.valueOf(10000))
                .workSchedules("{\"schedule\":\"test\"}")
                .contractStartDate(LocalDate.of(2024, 1, 1))
                .contractEndDate(LocalDate.of(2024, 12, 31))
                .paymentDay(25)
                .isActive(true)
                .payrollDeductionType(DeductionCalculator.PayrollDeductionType.PART_TIME_NONE)
                .build();
    }

    @Test
    @DisplayName("계약 정보 업데이트 - 모든 필드")
    void update_AllFields() {
        // given
        BigDecimal newHourlyWage = BigDecimal.valueOf(12000);
        String newWorkSchedules = "{\"schedule\":\"updated\"}";
        LocalDate newEndDate = LocalDate.of(2025, 12, 31);
        Integer newPaymentDay = 30;
        DeductionCalculator.PayrollDeductionType newType = DeductionCalculator.PayrollDeductionType.FREELANCER;

        // when
        contract.update(newHourlyWage, newWorkSchedules, newEndDate, newPaymentDay, newType);

        // then
        assertThat(contract.getHourlyWage()).isEqualTo(newHourlyWage);
        assertThat(contract.getWorkSchedules()).isEqualTo(newWorkSchedules);
        assertThat(contract.getContractEndDate()).isEqualTo(newEndDate);
        assertThat(contract.getPaymentDay()).isEqualTo(newPaymentDay);
        assertThat(contract.getPayrollDeductionType()).isEqualTo(newType);
    }

    @Test
    @DisplayName("계약 정보 업데이트 - 일부 필드만")
    void update_PartialFields() {
        // given
        BigDecimal originalHourlyWage = contract.getHourlyWage();
        String originalWorkSchedules = contract.getWorkSchedules();
        BigDecimal newHourlyWage = BigDecimal.valueOf(15000);

        // when
        contract.update(newHourlyWage, null, null, null, null);

        // then
        assertThat(contract.getHourlyWage()).isEqualTo(newHourlyWage);
        assertThat(contract.getWorkSchedules()).isEqualTo(originalWorkSchedules);
    }

    @Test
    @DisplayName("계약 종료")
    void terminate() {
        // when
        contract.terminate();

        // then
        assertThat(contract.getIsActive()).isFalse();
        assertThat(contract.getContractEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("계약 종료 - 이미 비활성화된 계약")
    void terminate_AlreadyInactive() {
        // given
        contract = WorkerContract.builder()
                .workplace(mockWorkplace)
                .worker(mockWorker)
                .hourlyWage(BigDecimal.valueOf(10000))
                .workSchedules("{}")
                .contractStartDate(LocalDate.of(2024, 1, 1))
                .paymentDay(25)
                .isActive(false)
                .build();

        // when
        contract.terminate();

        // then
        assertThat(contract.getIsActive()).isFalse();
        assertThat(contract.getContractEndDate()).isEqualTo(LocalDate.now());
    }
}
