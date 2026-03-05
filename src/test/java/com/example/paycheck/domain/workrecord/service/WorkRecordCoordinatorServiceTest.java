package com.example.paycheck.domain.workrecord.service;

import com.example.paycheck.common.exception.NotFoundException;
import com.example.paycheck.domain.allowance.entity.WeeklyAllowance;
import com.example.paycheck.domain.allowance.service.WeeklyAllowanceService;
import com.example.paycheck.domain.contract.entity.WorkerContract;
import com.example.paycheck.domain.salary.service.SalaryService;
import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordCoordinatorService 테스트")
class WorkRecordCoordinatorServiceTest {

    @Mock
    private WeeklyAllowanceService weeklyAllowanceService;

    @Mock
    private SalaryService salaryService;

    @InjectMocks
    private WorkRecordCoordinatorService coordinatorService;

    private WorkerContract mockContract;

    @BeforeEach
    void setUp() {
        mockContract = mock(WorkerContract.class);
        lenient().when(mockContract.getId()).thenReturn(1L);
        lenient().when(mockContract.getPaymentDay()).thenReturn(25);
    }

    private WorkRecord createMockWorkRecord(WorkRecordStatus status, LocalDate workDate, WeeklyAllowance allowance) {
        WorkRecord workRecord = mock(WorkRecord.class);
        lenient().when(workRecord.getStatus()).thenReturn(status);
        lenient().when(workRecord.getWorkDate()).thenReturn(workDate);
        lenient().when(workRecord.getContract()).thenReturn(mockContract);
        lenient().when(workRecord.getWeeklyAllowance()).thenReturn(allowance);
        return workRecord;
    }

    private WeeklyAllowance createMockAllowance(Long id) {
        WeeklyAllowance allowance = mock(WeeklyAllowance.class);
        lenient().when(allowance.getId()).thenReturn(id);
        lenient().when(allowance.getWorkRecords()).thenReturn(new ArrayList<>());
        return allowance;
    }

    @Nested
    @DisplayName("handleWorkRecordCreation - 근무 기록 생성 시")
    class HandleWorkRecordCreation {

        @Test
        @DisplayName("COMPLETED 상태 생성 - WeeklyAllowance 재계산 호출됨")
        void completedStatus_RecalculatesAllowance() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 10), allowance);

            // when
            coordinatorService.handleWorkRecordCreation(workRecord);

            // then
            verify(weeklyAllowanceService).recalculateAllowances(10L);
        }

        @Test
        @DisplayName("DELETED 상태 생성 - WeeklyAllowance 재계산 미호출")
        void deletedStatus_NoRecalculation() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.DELETED, LocalDate.of(2024, 1, 10), allowance);

            // when
            coordinatorService.handleWorkRecordCreation(workRecord);

            // then
            verify(weeklyAllowanceService, never()).recalculateAllowances(anyLong());
        }
    }

    @Nested
    @DisplayName("handleWorkRecordUpdate - 근무 기록 수정 시")
    class HandleWorkRecordUpdate {

        @Test
        @DisplayName("같은 WeeklyAllowance + SCHEDULED - allowance 재계산 1회, salary 미호출")
        void sameAllowance_Scheduled_OneRecalculation() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.SCHEDULED, LocalDate.of(2024, 1, 10), allowance);

            // when
            coordinatorService.handleWorkRecordUpdate(workRecord, allowance, allowance);

            // then
            verify(weeklyAllowanceService, times(1)).recalculateAllowances(10L);
            verify(salaryService, never()).recalculateSalaryAfterWorkRecordUpdate(anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("다른 WeeklyAllowance + COMPLETED - 양쪽 allowance 재계산 + salary 재계산")
        void differentAllowance_Completed_BothRecalculated() {
            // given
            WeeklyAllowance oldAllowance = createMockAllowance(10L);
            WeeklyAllowance newAllowance = createMockAllowance(20L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 10), newAllowance);

            // when
            coordinatorService.handleWorkRecordUpdate(workRecord, oldAllowance, newAllowance);

            // then
            verify(weeklyAllowanceService).recalculateAllowances(10L);
            verify(weeklyAllowanceService).recalculateAllowances(20L);
            verify(salaryService).recalculateSalaryAfterWorkRecordUpdate(eq(1L), anyInt(), anyInt());
        }

        @Test
        @DisplayName("COMPLETED 상태 수정 - salary 재계산 트리거됨")
        void completedStatus_SalaryRecalculated() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 10), allowance);

            // when
            coordinatorService.handleWorkRecordUpdate(workRecord, allowance, allowance);

            // then
            verify(salaryService).recalculateSalaryAfterWorkRecordUpdate(eq(1L), eq(2024), eq(1));
        }
    }

    @Nested
    @DisplayName("handleWorkRecordDeletion - 근무 기록 삭제 시")
    class HandleWorkRecordDeletion {

        @Test
        @DisplayName("빈 WorkRecords → WeeklyAllowance 삭제 호출")
        void emptyWorkRecords_DeletesAllowance() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            // workRecords는 이미 빈 ArrayList (createMockAllowance에서 설정)
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.SCHEDULED, LocalDate.of(2024, 1, 10), allowance);

            // when
            coordinatorService.handleWorkRecordDeletion(allowance, workRecord, WorkRecordStatus.SCHEDULED);

            // then
            verify(weeklyAllowanceService).deleteWeeklyAllowance(10L);
            verify(salaryService, never()).recalculateSalaryAfterWorkRecordUpdate(anyLong(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("WorkRecords 남아있음 + COMPLETED 삭제 - 재계산 + salary 재계산")
        void remainingWorkRecords_CompletedDeleted_Recalculate() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            allowance.getWorkRecords().add(mock(WorkRecord.class)); // 1개 남아있음
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 10), allowance);

            // when
            coordinatorService.handleWorkRecordDeletion(allowance, workRecord, WorkRecordStatus.COMPLETED);

            // then
            verify(weeklyAllowanceService).recalculateAllowances(10L);
            verify(salaryService).recalculateSalaryAfterWorkRecordUpdate(eq(1L), eq(2024), eq(1));
        }

        @Test
        @DisplayName("weeklyAllowance가 null이면 NPE 없이 정상 처리")
        void nullAllowance_NoCrash() {
            // given
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.SCHEDULED, LocalDate.of(2024, 1, 10), null);

            // when & then
            assertThatCode(() ->
                    coordinatorService.handleWorkRecordDeletion(null, workRecord, WorkRecordStatus.SCHEDULED)
            ).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("급여 귀속월 판정 - handleWorkRecordCompletion 통해 검증")
    class SalaryMonthDetermination {

        @Test
        @DisplayName("workDate 24일, paymentDay 25 → 당월(1월) 급여에 귀속")
        void beforePaymentDay_CurrentMonth() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 24), allowance);

            // when
            coordinatorService.handleWorkRecordCompletion(workRecord);

            // then
            verify(salaryService).recalculateSalaryAfterWorkRecordUpdate(1L, 2024, 1);
        }

        @Test
        @DisplayName("workDate 25일, paymentDay 25 → 다음달(2월) 급여에 귀속")
        void onPaymentDay_NextMonth() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 25), allowance);

            // when
            coordinatorService.handleWorkRecordCompletion(workRecord);

            // then
            verify(salaryService).recalculateSalaryAfterWorkRecordUpdate(1L, 2024, 2);
        }

        @Test
        @DisplayName("12월 25일, paymentDay 25 → 2025년 1월 급여에 귀속 (연도 넘김)")
        void december_YearCrossover() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 12, 25), allowance);

            // when
            coordinatorService.handleWorkRecordCompletion(workRecord);

            // then
            verify(salaryService).recalculateSalaryAfterWorkRecordUpdate(1L, 2025, 1);
        }
    }

    @Nested
    @DisplayName("예외 처리 및 배치")
    class ExceptionAndBatch {

        @Test
        @DisplayName("NotFoundException이 발생해도 예외가 전파되지 않음 (급여 미생성 상태)")
        void notFoundExceptionSwallowed() {
            // given
            WeeklyAllowance allowance = createMockAllowance(10L);
            WorkRecord workRecord = createMockWorkRecord(WorkRecordStatus.COMPLETED, LocalDate.of(2024, 1, 10), allowance);
            doThrow(new NotFoundException(null, "급여 미생성")).when(salaryService)
                    .recalculateSalaryAfterWorkRecordUpdate(anyLong(), anyInt(), anyInt());

            // when & then
            assertThatCode(() ->
                    coordinatorService.handleWorkRecordCompletion(workRecord)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("handleBatchWorkRecordCompletion - 같은 계약/년월 5개 → 1회만 재계산")
        void batchCompletion_SameContractMonth_OneRecalculation() {
            // given
            List<WorkRecord> records = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                WorkRecord wr = createMockWorkRecord(
                        WorkRecordStatus.COMPLETED,
                        LocalDate.of(2024, 1, 10 + i), // 모두 1월 10~14일 (paymentDay=25 미만 → 당월)
                        createMockAllowance((long) (10 + i)));
                records.add(wr);
            }

            // when
            coordinatorService.handleBatchWorkRecordCompletion(records);

            // then - 같은 계약(1L), 같은 년월(2024/1)이므로 1회만 호출
            verify(salaryService, times(1)).recalculateSalaryAfterWorkRecordUpdate(1L, 2024, 1);
        }
    }
}
