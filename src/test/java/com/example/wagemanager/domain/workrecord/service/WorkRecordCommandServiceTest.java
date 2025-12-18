package com.example.wagemanager.domain.workrecord.service;

import com.example.wagemanager.common.exception.BadRequestException;
import com.example.wagemanager.common.exception.NotFoundException;
import com.example.wagemanager.domain.allowance.entity.WeeklyAllowance;
import com.example.wagemanager.domain.contract.entity.WorkerContract;
import com.example.wagemanager.domain.contract.repository.WorkerContractRepository;
import com.example.wagemanager.domain.user.entity.User;
import com.example.wagemanager.domain.workrecord.dto.WorkRecordDto;
import com.example.wagemanager.domain.workrecord.entity.WorkRecord;
import com.example.wagemanager.domain.workrecord.enums.WorkRecordStatus;
import com.example.wagemanager.domain.workrecord.repository.WorkRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkRecordCommandService 테스트")
class WorkRecordCommandServiceTest {

    @Mock
    private WorkRecordRepository workRecordRepository;

    @Mock
    private WorkerContractRepository workerContractRepository;

    @Mock
    private WorkRecordCoordinatorService coordinatorService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WorkRecordCommandService workRecordCommandService;

    private WorkerContract testContract;
    private WorkRecord testWorkRecord;
    private WeeklyAllowance testWeeklyAllowance;

    @BeforeEach
    void setUp() {
        testWeeklyAllowance = mock(WeeklyAllowance.class);
    }

    @Test
    @DisplayName("고용주의 근무 일정 생성 성공 - 미래 날짜 (SCHEDULED)")
    void createWorkRecordByEmployer_Success_Future() {
        // given
        testContract = mock(WorkerContract.class);
        testWorkRecord = mock(WorkRecord.class);
        User worker = mock(User.class);
        com.example.wagemanager.domain.worker.entity.Worker workerEntity = mock(com.example.wagemanager.domain.worker.entity.Worker.class);

        WorkRecordDto.CreateRequest request = WorkRecordDto.CreateRequest.builder()
                .contractId(1L)
                .workDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .memo("테스트 메모")
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.of(testContract));
        when(coordinatorService.getOrCreateWeeklyAllowance(any(), any())).thenReturn(testWeeklyAllowance);
        when(workRecordRepository.save(any(WorkRecord.class))).thenReturn(testWorkRecord);
        when(testWorkRecord.getId()).thenReturn(1L);
        when(testWorkRecord.getContract()).thenReturn(testContract);
        when(testContract.getWorker()).thenReturn(workerEntity);
        when(workerEntity.getUser()).thenReturn(worker);

        // when
        WorkRecordDto.Response result = workRecordCommandService.createWorkRecordByEmployer(request);

        // then
        assertThat(result).isNotNull();
        verify(workRecordRepository).save(any(WorkRecord.class));
    }

    @Test
    @DisplayName("고용주의 근무 일정 생성 실패 - 계약 없음")
    void createWorkRecordByEmployer_Fail_ContractNotFound() {
        // given
        WorkRecordDto.CreateRequest request = WorkRecordDto.CreateRequest.builder()
                .contractId(999L)
                .workDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workRecordCommandService.createWorkRecordByEmployer(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("근무 기록 업데이트 실패 - 기록 없음")
    void updateWorkRecord_NotFound() {
        // given
        WorkRecordDto.UpdateRequest request = WorkRecordDto.UpdateRequest.builder()
                .startTime(LocalTime.of(10, 0))
                .build();

        when(workRecordRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workRecordCommandService.updateWorkRecord(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("근무 완료 처리 성공")
    void completeWorkRecord_Success() {
        // given
        testWorkRecord = mock(WorkRecord.class);
        when(workRecordRepository.findById(anyLong())).thenReturn(Optional.of(testWorkRecord));

        // when
        workRecordCommandService.completeWorkRecord(1L);

        // then
        verify(testWorkRecord).complete();
        verify(coordinatorService).handleWorkRecordCompletion(testWorkRecord);
    }

    @Test
    @DisplayName("근무 일정 일괄 생성 성공")
    void createWorkRecordsBatch_Success() {
        // given
        testContract = mock(WorkerContract.class);
        User worker = mock(User.class);
        com.example.wagemanager.domain.worker.entity.Worker workerEntity = mock(com.example.wagemanager.domain.worker.entity.Worker.class);

        WorkRecordDto.BatchCreateRequest request = WorkRecordDto.BatchCreateRequest.builder()
                .contractId(1L)
                .workDates(Arrays.asList(
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(2),
                        LocalDate.now().plusDays(3)
                ))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .breakMinutes(60)
                .memo("일괄 생성 메모")
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.of(testContract));
        when(workRecordRepository.existsByContractAndWorkDate(any(), any())).thenReturn(false);
        when(coordinatorService.getOrCreateWeeklyAllowance(any(), any())).thenReturn(testWeeklyAllowance);
        when(workRecordRepository.save(any(WorkRecord.class))).thenReturn(mock(WorkRecord.class));
        when(testContract.getWorker()).thenReturn(workerEntity);
        when(workerEntity.getUser()).thenReturn(worker);

        // when
        WorkRecordDto.BatchCreateResponse result = workRecordCommandService.createWorkRecordsBatch(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCreatedCount()).isEqualTo(3);
        assertThat(result.getSkippedCount()).isEqualTo(0);
        assertThat(result.getTotalRequested()).isEqualTo(3);
        verify(workRecordRepository, times(3)).save(any(WorkRecord.class));
    }

    @Test
    @DisplayName("근무 일정 일괄 생성 - 중복 스킵")
    void createWorkRecordsBatch_WithDuplicates() {
        // given
        testContract = mock(WorkerContract.class);
        User worker = mock(User.class);
        com.example.wagemanager.domain.worker.entity.Worker workerEntity = mock(com.example.wagemanager.domain.worker.entity.Worker.class);

        WorkRecordDto.BatchCreateRequest request = WorkRecordDto.BatchCreateRequest.builder()
                .contractId(1L)
                .workDates(Arrays.asList(
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(2)
                ))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        when(workerContractRepository.findById(anyLong())).thenReturn(Optional.of(testContract));
        // 첫 번째는 중복, 두 번째는 새로운 기록
        when(workRecordRepository.existsByContractAndWorkDate(any(), any()))
                .thenReturn(true, false);
        when(coordinatorService.getOrCreateWeeklyAllowance(any(), any())).thenReturn(testWeeklyAllowance);
        when(workRecordRepository.save(any(WorkRecord.class))).thenReturn(mock(WorkRecord.class));
        when(testContract.getWorker()).thenReturn(workerEntity);
        when(workerEntity.getUser()).thenReturn(worker);

        // when
        WorkRecordDto.BatchCreateResponse result = workRecordCommandService.createWorkRecordsBatch(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(workRecordRepository, times(1)).save(any(WorkRecord.class));
    }
}
