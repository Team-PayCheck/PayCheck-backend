package com.example.wagemanager.domain.workrecord.entity;

import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WorkRecord 엔티티 테스트")
class WorkRecordTest {

    private WorkRecord workRecord;
    private WorkerContract mockContract;

    @BeforeEach
    void setUp() {
        mockContract = mock(WorkerContract.class);
        when(mockContract.getHourlyWage()).thenReturn(BigDecimal.valueOf(10000));

        workRecord = WorkRecord.builder()
                .id(1L)
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .status(WorkRecordStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("WeeklyAllowance 할당")
    void assignToWeeklyAllowance() {
        // given
        WeeklyAllowance weeklyAllowance = mock(WeeklyAllowance.class);

        // when
        workRecord.assignToWeeklyAllowance(weeklyAllowance);

        // then
        assertThat(workRecord.getWeeklyAllowance()).isEqualTo(weeklyAllowance);
    }

    @Test
    @DisplayName("근무 시간 수정 - SCHEDULED 상태")
    void updateWorkTime_Scheduled() {
        // given
        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(19, 0);
        String memo = "시간 변경";

        // when
        workRecord.updateWorkTime(newStart, newEnd, memo);

        // then
        assertThat(workRecord.getStartTime()).isEqualTo(newStart);
        assertThat(workRecord.getEndTime()).isEqualTo(newEnd);
        assertThat(workRecord.getMemo()).isEqualTo(memo);
        assertThat(workRecord.getIsModified()).isTrue();
    }

    @Test
    @DisplayName("근무 시간 수정 - COMPLETED 상태 (재계산 발생)")
    void updateWorkTime_Completed() {
        // given
        workRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.of(2024, 1, 15))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkRecordStatus.COMPLETED)
                .build();

        LocalTime newStart = LocalTime.of(10, 0);
        LocalTime newEnd = LocalTime.of(19, 0);

        // when
        workRecord.updateWorkTime(newStart, newEnd, "시간 변경");

        // then
        assertThat(workRecord.getStartTime()).isEqualTo(newStart);
        assertThat(workRecord.getEndTime()).isEqualTo(newEnd);
        assertThat(workRecord.getIsModified()).isTrue();
    }

    @Test
    @DisplayName("근무 기록 수정")
    void updateWorkRecord() {
        // given
        LocalTime newStart = LocalTime.of(8, 0);
        LocalTime newEnd = LocalTime.of(17, 0);
        Integer newBreakMinutes = 30;
        Integer newTotalWorkMinutes = 510;
        String memo = "근무 기록 수정";

        // when
        workRecord.updateWorkRecord(newStart, newEnd, newBreakMinutes, newTotalWorkMinutes, memo);

        // then
        assertThat(workRecord.getStartTime()).isEqualTo(newStart);
        assertThat(workRecord.getEndTime()).isEqualTo(newEnd);
        assertThat(workRecord.getBreakMinutes()).isEqualTo(newBreakMinutes);
        assertThat(workRecord.getTotalWorkMinutes()).isEqualTo(newTotalWorkMinutes);
        assertThat(workRecord.getMemo()).isEqualTo(memo);
        assertThat(workRecord.getIsModified()).isTrue();
    }

    @Test
    @DisplayName("근무 완료 처리")
    void complete() {
        // when
        workRecord.complete();

        // then
        assertThat(workRecord.getStatus()).isEqualTo(WorkRecordStatus.COMPLETED);
    }

    @Test
    @DisplayName("승인 처리 - 과거 날짜 (COMPLETED)")
    void approve_PastDate() {
        // given
        workRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.now().minusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkRecordStatus.PENDING_APPROVAL)
                .build();

        // when
        workRecord.approve();

        // then
        assertThat(workRecord.getStatus()).isEqualTo(WorkRecordStatus.COMPLETED);
    }

    @Test
    @DisplayName("승인 처리 - 미래 날짜 (SCHEDULED)")
    void approve_FutureDate() {
        // given
        workRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkRecordStatus.PENDING_APPROVAL)
                .build();

        // when
        workRecord.approve();

        // then
        assertThat(workRecord.getStatus()).isEqualTo(WorkRecordStatus.SCHEDULED);
    }

    @Test
    @DisplayName("승인 처리 실패 - 잘못된 상태")
    void approve_InvalidStatus() {
        // given
        workRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkRecordStatus.COMPLETED)
                .build();

        // when & then
        assertThatThrownBy(() -> workRecord.approve())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인 대기 상태만 승인할 수 있습니다");
    }

    @Test
    @DisplayName("상태 확인 - PENDING_APPROVAL")
    void isPendingApproval_True() {
        // given
        workRecord = WorkRecord.builder()
                .contract(mockContract)
                .workDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkRecordStatus.PENDING_APPROVAL)
                .build();

        // when
        boolean result = workRecord.isPendingApproval();

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("상태 확인 - Not PENDING_APPROVAL")
    void isPendingApproval_False() {
        // when
        boolean result = workRecord.isPendingApproval();

        // then
        assertThat(result).isFalse();
    }
}
