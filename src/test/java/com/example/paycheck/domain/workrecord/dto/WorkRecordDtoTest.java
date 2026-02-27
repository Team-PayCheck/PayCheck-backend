package com.example.paycheck.domain.workrecord.dto;

import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.user.entity.User;
import com.example.paycheck.domain.worker.entity.Worker;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workplace.entity.Workplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("WorkRecordDto 테스트")
class WorkRecordDtoTest {

    private WorkRecord workRecord;
    private WorkerContract mockContract;
    private Worker mockWorker;
    private User mockUser;
    private Workplace mockWorkplace;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("홍길동");

        mockWorker = mock(Worker.class);
        when(mockWorker.getUser()).thenReturn(mockUser);
        when(mockWorker.getWorkerCode()).thenReturn("WORKER001");

        mockWorkplace = mock(Workplace.class);
        when(mockWorkplace.getName()).thenReturn("테스트 사업장");

        mockContract = mock(WorkerContract.class);
        when(mockContract.getId()).thenReturn(1L);
        when(mockContract.getWorker()).thenReturn(mockWorker);
        when(mockContract.getWorkplace()).thenReturn(mockWorkplace);
        when(mockContract.getHourlyWage()).thenReturn(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("DetailedResponse.from() - 급여 필드 매핑 테스트")
    void detailedResponse_from_shouldMapSalaryFields() {
        // given
        workRecord = WorkRecord.builder()
                .id(1L)
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .totalWorkMinutes(480)
                .status(WorkRecordStatus.COMPLETED)
                .isModified(false)
                .memo("테스트 메모")
                .baseSalary(BigDecimal.valueOf(80000))
                .nightSalary(BigDecimal.valueOf(15000))
                .holidaySalary(BigDecimal.valueOf(20000))
                .totalSalary(BigDecimal.valueOf(115000))
                .build();

        // when
        WorkRecordDto.DetailedResponse response = WorkRecordDto.DetailedResponse.from(workRecord);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getContractId()).isEqualTo(1L);
        assertThat(response.getWorkerName()).isEqualTo("홍길동");
        assertThat(response.getWorkerCode()).isEqualTo("WORKER001");
        assertThat(response.getWorkplaceName()).isEqualTo("테스트 사업장");
        assertThat(response.getWorkDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.getBreakMinutes()).isEqualTo(60);
        assertThat(response.getTotalWorkMinutes()).isEqualTo(480);
        assertThat(response.getStatus()).isEqualTo(WorkRecordStatus.COMPLETED);
        assertThat(response.getIsModified()).isFalse();
        assertThat(response.getMemo()).isEqualTo("테스트 메모");

        // 시급 및 급여 필드 검증
        assertThat(response.getHourlyWage()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(response.getBaseSalary()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(response.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        assertThat(response.getHolidaySalary()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(response.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(115000));
    }

    @Test
    @DisplayName("DetailedResponse.from() - 급여가 0인 경우")
    void detailedResponse_from_shouldHandleZeroSalary() {
        // given
        workRecord = WorkRecord.builder()
                .id(2L)
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 16))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .status(WorkRecordStatus.SCHEDULED)
                .build();

        // when
        WorkRecordDto.DetailedResponse response = WorkRecordDto.DetailedResponse.from(workRecord);

        // then
        assertThat(response.getHourlyWage()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(response.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getNightSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalSalary()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("DetailedResponse.from() - 야간/휴일 수당만 있는 경우")
    void detailedResponse_from_shouldHandleOnlyNightAndHolidaySalary() {
        // given
        workRecord = WorkRecord.builder()
                .id(3L)
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 14)) // Sunday
                .startTime(LocalTime.of(22, 0))
                .endTime(LocalTime.of(6, 0))
                .breakMinutes(0)
                .status(WorkRecordStatus.COMPLETED)
                .baseSalary(BigDecimal.ZERO)
                .nightSalary(BigDecimal.valueOf(160000))
                .holidaySalary(BigDecimal.ZERO)
                .totalSalary(BigDecimal.valueOf(160000))
                .build();

        // when
        WorkRecordDto.DetailedResponse response = WorkRecordDto.DetailedResponse.from(workRecord);

        // then
        assertThat(response.getBaseSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getNightSalary()).isEqualByComparingTo(BigDecimal.valueOf(160000));
        assertThat(response.getHolidaySalary()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalSalary()).isEqualByComparingTo(BigDecimal.valueOf(160000));
    }
}
