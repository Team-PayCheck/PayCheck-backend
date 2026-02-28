package com.example.paycheck.domain.workrecord.scheduler;

import com.example.paycheck.domain.workrecord.entity.WorkRecord;
import com.example.paycheck.domain.workrecord.enums.WorkRecordStatus;
import com.example.paycheck.domain.workrecord.repository.WorkRecordRepository;
import com.example.paycheck.domain.workrecord.service.WorkRecordCommandService;
import com.example.paycheck.domain.workrecord.service.WorkRecordGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordScheduler 테스트")
class WorkRecordSchedulerTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkRecordGenerationService workRecordGenerationService;

    @Mock
    private WorkRecordCommandService workRecordCommandService;

    @InjectMocks
    private WorkRecordScheduler workRecordScheduler;

    @Test
    @DisplayName("종료된 SCHEDULED 근무 자동 완료 성공")
    void autoCompletePastScheduledWorkRecords_Success() {
        // given
        WorkRecord first = WorkRecord.builder().id(1L).build();
        WorkRecord second = WorkRecord.builder().id(2L).build();

        when(workRecordRepository.findPastScheduledWorkRecords(
                eq(WorkRecordStatus.SCHEDULED),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        // when
        workRecordScheduler.autoCompletePastScheduledWorkRecords();

        // then
        verify(workRecordCommandService).completeWorkRecord(first);
        verify(workRecordCommandService).completeWorkRecord(second);
    }

    @Test
    @DisplayName("종료된 SCHEDULED 근무 자동 완료 - 대상 없음")
    void autoCompletePastScheduledWorkRecords_NoTargets() {
        // given
        when(workRecordRepository.findPastScheduledWorkRecords(
                eq(WorkRecordStatus.SCHEDULED),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // when
        workRecordScheduler.autoCompletePastScheduledWorkRecords();

        // then
        verify(workRecordCommandService, never()).completeWorkRecord(any(WorkRecord.class));
    }
}
